/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.jifa.profile.lang.java.extractor;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.profile.lang.java.common.EventConstant;
import org.eclipse.jifa.profile.model.TaskData;
import org.eclipse.jifa.profile.lang.java.model.jfr.RecordedEvent;
import org.eclipse.jifa.profile.lang.java.model.jfr.RecordedStackTrace;
import org.eclipse.jifa.profile.lang.java.model.jfr.RecordedThread;
import org.eclipse.jifa.profile.lang.java.util.GCUtil;
import org.eclipse.jifa.profile.lang.java.util.StackTraceUtil;
import org.eclipse.jifa.profile.lang.java.util.TimeUtil;
import org.eclipse.jifa.profile.model.DimensionResult;
import org.eclipse.jifa.profile.lang.java.model.AnalysisResult;
import org.eclipse.jifa.profile.model.TaskCPUTime;
import org.eclipse.jifa.profile.lang.java.model.JavaThreadCPUTime;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CPUTimeExtractor extends Extractor {

    private static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<String>() {
        {
            add(EventConstant.UNSIGNED_INT_FLAG);
            add(EventConstant.GARBAGE_COLLECTION);
            add(EventConstant.ACTIVE_SETTING);
            add(EventConstant.CPU_INFORMATION);
            add(EventConstant.CPC_RUNTIME_INFORMATION);
            add(EventConstant.ENV_VAR);
            add(EventConstant.THREAD_START);
            add(EventConstant.THREAD_CPU_LOAD);
            add(EventConstant.EXECUTION_SAMPLE);
        }
    });

    static class CpuTaskData extends TaskData {
        CpuTaskData(RecordedThread thread) {
            super(thread);
        }

        Instant start;

        long user = 0;

        long system = 0;

        long sampleCount;

        boolean firstThreadCPULoadEventIsFired;

        Map<String, Long> vmOperations;
    }

    private static final int ASYNC_PROFILER_DEFAULT_INTERVAL = 10 * 1000 * 1000;
    private final Map<Long, CpuTaskData> data = new HashMap<>();

    private long period = -1;

    private long threadCPULoadEventId = -1;
    private boolean executionSampleByJFR = true;

    private int cpuCores;
    private long intervalAsync; // unit: nano
    private long intervalJFR; // unit: nano

    private int concurrentGCThreads = -1;
    private int parallelGCThreads = -1;
    private long concurrentGCWallTime = 0;
    private long parallelGCWallTime = 0;
    private long serialGCWallTime = 0;

    private boolean isWallClock = false;

    public CPUTimeExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);

        Long id = context.getEventTypeId(EventConstant.THREAD_CPU_LOAD);
        if (id != null) {
            threadCPULoadEventId = id;
        }
    }

    private long calcProcCpuLoadPeriod(JFRAnalysisContext context) {
        List<RecordedEvent> events = context.getEvents();
        List<RecordedEvent> cpuLoadEvents = events.stream().filter(
                item -> EventConstant.PROCESS_CPU_LOAD.equals(item.getEventType().name())).toList();
        if (cpuLoadEvents.size() < 2) {
            throw new RuntimeException(String.format("at least 2 %s events needed", EventConstant.PROCESS_CPU_LOAD));
        }

        long start = cpuLoadEvents.get(0).getStartTimeNanos();
        long end = cpuLoadEvents.get(cpuLoadEvents.size() - 1).getStartTimeNanos();
        return (end - start) / (cpuLoadEvents.size() - 1);
    }

    CpuTaskData getThreadData(RecordedThread thread) {
        return data.computeIfAbsent(thread.getJavaThreadId(), i -> new CpuTaskData(thread));
    }

    CpuTaskData getFakeThread() {
        long threadId = -1L;
        return data.computeIfAbsent(threadId, i -> new CpuTaskData(
                new RecordedThread(-1L, "Fake$Thread", -1L, -1L)));
    }

    private void updatePeriod(String value) {
        period = TimeUtil.parseTimespan(value);
    }

    @Override
    void visitUnsignedIntFlag(RecordedEvent event) {
        String name = event.getString("name");
        if ("ConcGCThreads".equals(name)) {
            concurrentGCThreads = event.getInt("value");
        } else if ("ParallelGCThreads".equals(name)) {
            parallelGCThreads = event.getInt("value");
        }
    }

    @Override
    void visitGarbageCollection(RecordedEvent event) {
        String name = event.getString("name");
        long duration = event.getDuration().toNanos();

        if (GCUtil.isParallelGC(name)) {
            parallelGCWallTime += duration;
        } else if (GCUtil.isConcGC(name)) {
            concurrentGCWallTime += duration;
        } else if (GCUtil.isSerialGC(name)) {
            serialGCWallTime += duration;
        }
    }

    @Override
    void visitActiveSetting(RecordedEvent event) {
        if (event.getSettingFor().getEventId() == threadCPULoadEventId
                && EventConstant.PERIOD.equals(event.getString("name"))) {
            updatePeriod(event.getValue("value"));
        }

        if (this.context.isExecutionSampleEventTypeId(event.getSettingFor().getEventId())) {
            if (EventConstant.WALL.equals(event.getString("name"))) {
                this.isWallClock = true;
            } else if (EventConstant.INTERVAL.equals(event.getString("name"))) {
                // async-profiler is "interval"
                this.intervalAsync = Long.parseLong(event.getString("value"));
                this.executionSampleByJFR = false;
            } else if (EventConstant.PERIOD.equals(event.getString("name"))) {
                // JFR is "period"
                try {
                    this.intervalJFR = TimeUtil.parseTimespan(event.getString("value"));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    void visitCPUInformation(RecordedEvent event) {
        if (cpuCores == 0) {
            cpuCores = event.getInt("hwThreads");
        }
    }

    @Override
    void visitCPCRuntimeInformation(RecordedEvent event) {
        if (event.getInt("availableProcessors") > 0) {
            cpuCores = event.getInt("availableProcessors");
        }
    }

    @Override
    void visitEnvVar(RecordedEvent event) {
        if ("CPU_COUNT".equals(event.getString("key"))) {
            cpuCores = Integer.parseInt(event.getString("value"));
        }
    }

    @Override
    void visitThreadStart(RecordedEvent event) {
        if (event.getThread() == null) {
            return;
        }
        CpuTaskData cpuTaskData = getThreadData(event.getThread());
        cpuTaskData.start = event.getStartTime();
    }

    @Override
    void visitThreadCPULoad(RecordedEvent event) {
        if (event.getThread() == null) {
            return;
        }
        CpuTaskData cpuTaskData = getThreadData(event.getThread());
        long nanos = period;
        if (!cpuTaskData.firstThreadCPULoadEventIsFired) {
            if (cpuTaskData.start != null) {
                Duration between = Duration.between(cpuTaskData.start, event.getStartTime());
                nanos = Math.min(nanos, between.toNanos());
            }
            cpuTaskData.firstThreadCPULoadEventIsFired = true;
        }
        cpuTaskData.user += (long) (event.getFloat("user") * nanos);
        cpuTaskData.system += (long) (event.getFloat("system") * nanos);
    }

    @Override
    void visitProcessCPULoad(RecordedEvent event) {
        if (period == -1) {
            throw new IllegalStateException("invalid period");
        }
        CpuTaskData cpuTaskData = getFakeThread();
        long nanos = period;
        cpuTaskData.user += (long) (event.getFloat("jvmUser") * nanos);
        cpuTaskData.system += (long) (event.getFloat("jvmSystem") * nanos);

        throw new RuntimeException("should not reach here");
    }

    @Override
    void visitExecutionSample(RecordedEvent event) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            return;
        }

        RecordedThread thread = event.getThread("eventThread");
        if (thread == null) {
            thread = event.getThread("sampledThread");
        }
        if (thread == null) {
            return;
        }
        CpuTaskData cpuTaskData = getThreadData(thread);

        if (cpuTaskData.getSamples() == null) {
            cpuTaskData.setSamples(new HashMap<>());
        }

        cpuTaskData.getSamples().compute(stackTrace, (k, count) -> count == null ? 1 : count + 1);
        cpuTaskData.sampleCount++;
    }

    @Override
    void visitExecuteVMOperation(RecordedEvent event) {
        RecordedThread caller = event.getThread("caller");
        if (caller == null) {
            caller = event.getThread("eventThread");
        }

        CpuTaskData cpuTaskData = getThreadData(caller);

        String operation = event.getString("operation");
        if (cpuTaskData.vmOperations == null) {
            cpuTaskData.vmOperations = new HashMap<>();
        }

        long duration = event.getDuration().toNanos();
        cpuTaskData.vmOperations.compute(operation, (k, d) -> duration + (d == null ? 0 : d));
    }

    private boolean isSerialVMOperation(String name) {
        return "HandshakeOneThread".equals(name);
    }

    private List<TaskCPUTime> buildThreadCPUTime() {
        List<TaskCPUTime> threadCPUTimes = new ArrayList<>();
        if (this.isWallClock) {
            return threadCPUTimes;
        }
        for (CpuTaskData data : this.data.values()) {
            if (data.getSamples() == null && data.vmOperations == null) {
                continue;
            }
            JavaThreadCPUTime threadCPUTime = new JavaThreadCPUTime();
            threadCPUTime.setTask(context.getThread(data.getThread()));

            if (data.getSamples() != null) {
                if (this.executionSampleByJFR) {
                    if (intervalJFR <= 0) {
                        throw new RuntimeException("need profiling interval to calculate approximate CPU time");
                    }
                    long cpuTimeMax = (data.user + data.system) * cpuCores;
                    long sampleTime = data.sampleCount * intervalJFR;
                    threadCPUTime.setUser(Math.min(sampleTime, cpuTimeMax));
                    threadCPUTime.setSystem(0);
                } else {
                    if (intervalAsync <= 0) {
                        String interval = System.getProperty("asyncProfilerCpuIntervalMs");
                        if (interval != null && !interval.isEmpty()) {
                            try {
                                intervalAsync = Long.parseLong(interval) * 1000 * 1000;
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                        if (intervalAsync <= 0) {
                            log.info("use default cpu interval 10ms");
                            intervalAsync = ASYNC_PROFILER_DEFAULT_INTERVAL;
                        }
                    }
                    threadCPUTime.setUser(data.sampleCount * intervalAsync);
                    threadCPUTime.setSystem(0);
                }

                threadCPUTime.setSamples(data.getSamples().entrySet().stream().collect(
                        Collectors.toMap(
                                e -> StackTraceUtil.build(e.getKey(), context.getSymbols()),
                                Map.Entry::getValue,
                                Long::sum)
                ));
            }

            if (data.vmOperations != null && this.executionSampleByJFR) {
                Map<String, Long> vmOperations = new HashMap<>();
                for (Map.Entry<String, Long> vmOperation : data.vmOperations.entrySet()) {
                    vmOperations.put(
                            vmOperation.getKey(),
                            vmOperation.getValue() * (isSerialVMOperation(vmOperation.getKey()) ? 1 : cpuCores)
                    );
                }
                threadCPUTime.setVmOperations(vmOperations);
            }

            threadCPUTimes.add(threadCPUTime);
        }

        threadCPUTimes.sort((o1, o2) -> {
            long delta = o2.totalCPUTime() - o1.totalCPUTime();
            return delta > 0 ? 1 : (delta == 0 ? 0 : -1);
        });
        return threadCPUTimes;
    }

    private long buildGCCpuTime() {
        if (parallelGCThreads < 0 || concurrentGCThreads < 0) {
            log.warn("invalid ParallelGCThreads or ConcurrentGCThreads, GC cpu time can not be calculated");
            return -1;
        } else {
            return parallelGCThreads * parallelGCWallTime + concurrentGCThreads * concurrentGCWallTime + serialGCWallTime;
        }
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskCPUTime> cpuResult = new DimensionResult<>();
        cpuResult.setList(buildThreadCPUTime());
        result.setCpuTime(cpuResult);
        if (this.executionSampleByJFR) {
            result.setGcCPUTime(buildGCCpuTime());
        }
    }
}
