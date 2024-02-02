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
package org.eclipse.jifa.profile.lang.java;

import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.eclipse.jifa.profile.api.ProfileAnalyzer;
import org.eclipse.jifa.profile.enums.Language;
import org.eclipse.jifa.profile.context.ProfileAnalysisContext;
import org.eclipse.jifa.profile.lang.java.common.ProfileDimension;
import org.eclipse.jifa.profile.lang.java.model.AnalysisResult;
import org.eclipse.jifa.profile.lang.java.request.AnalysisRequest;
import org.eclipse.jifa.profile.lang.java.request.DimensionBuilder;
import org.eclipse.jifa.profile.lang.java.util.VMOperationUtil;
import org.eclipse.jifa.profile.lang.PerfDimensionFactory;
import org.eclipse.jifa.profile.lang.java.model.JavaThreadCPUTime;
import org.eclipse.jifa.profile.model.*;
import org.eclipse.jifa.profile.vo.BasicMetadata;
import org.eclipse.jifa.profile.vo.FlameGraph;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unchecked")
public class JavaProfileAnalyzerImpl implements ProfileAnalyzer {

    private final ProfileAnalysisContext<AnalysisResult> context;

    public JavaProfileAnalyzerImpl(Path path, Map<String, String> options, ProgressListener listener) {
        this.context = new ProfileAnalysisContext<>(
                $(() -> {
                    JFRAnalyzerImpl analyzer = new JFRAnalyzerImpl();
                    AnalysisRequest request = new AnalysisRequest(path, DimensionBuilder.ALL);
                    return analyzer.execute(request, listener);
                })
        );
    }

    @Override
    public FlameGraph getFlameGraph(String dimension, boolean include, List<String> taskSet) {
        return $(() -> {
                    AnalysisResult result = context.getSnapshot();
                    return createFlameGraph(ProfileDimension.of(dimension), result, include, taskSet);
                }
        );
    }

    @Override
    public BasicMetadata basic() {
        BasicMetadata basic = new BasicMetadata();
        basic.setLanguage(Language.JAVA);
        basic.setPerfDimensions(PerfDimensionFactory.MAP.get(Language.JAVA));
        return basic;
    }

    private FlameGraph createFlameGraph(ProfileDimension dimension, AnalysisResult result, boolean include,
                                        List<String> taskSet) {
        List<Object[]> os = new ArrayList<>();
        Map<String, Long> names = new HashMap<>();
        SymbolMap symbolTable = new SymbolMap();
        if (dimension == ProfileDimension.CPU) {
            DimensionResult<TaskCPUTime> cpuTime = result.getCpuTime();
            long gcCPUTime = result.getGcCPUTime();
            generateCpuTime(cpuTime, gcCPUTime, os, names, symbolTable, include, taskSet);
        } else {
            DimensionResult<? extends TaskResultBase> DimensionResult = switch (dimension) {
                case CPU_SAMPLE -> result.getCpuSample();
                case WALL_CLOCK -> result.getWallClock();
                case NATIVE_EXECUTION_SAMPLES -> result.getNativeExecutionSamples();
                case ALLOC -> result.getAllocations();
                case MEM -> result.getAllocatedMemory();
                case FILE_IO_TIME -> result.getFileIOTime();
                case FILE_READ_SIZE -> result.getFileReadSize();
                case FILE_WRITE_SIZE -> result.getFileWriteSize();
                case SOCKET_READ_TIME -> result.getSocketReadTime();
                case SOCKET_READ_SIZE -> result.getSocketReadSize();
                case SOCKET_WRITE_TIME -> result.getSocketWriteTime();
                case SOCKET_WRITE_SIZE -> result.getSocketWriteSize();
                case LOCK_ACQUIRE -> result.getLockAcquire();
                case LOCK_WAIT_TIME -> result.getLockWaitTime();
                case SYNCHRONIZATION -> result.getSynchronization();
                case CLASS_LOAD_COUNT -> result.getClassLoadCount();
                case CLASS_LOAD_WALL_TIME -> result.getClassLoadWallTime();
                case THREAD_SLEEP -> result.getThreadSleepTime();
                default -> throw new RuntimeException("should not reach here");
            };
            generate(DimensionResult, os, names, symbolTable, include, taskSet);
        }

        FlameGraph fg = new FlameGraph();
        fg.setData(os.toArray(new Object[0][]));
        fg.setThreadSplit(names);
        fg.setSymbolTable(symbolTable.getReverseMap());
        return fg;
    }

    private void generate(DimensionResult<? extends TaskResultBase> result, List<Object[]> os, Map<String, Long> names,
                          SymbolMap map, boolean include, List<String> taskSet) {
        List<? extends TaskResultBase> list = result.getList();
        Set<String> set = null;
        if (taskSet != null) {
            set = new HashSet<>(taskSet);
        }
        for (TaskResultBase ts : list) {
            if (set != null && !set.isEmpty()) {
                if (include && !set.contains(ts.getTask().getName())) {
                    continue;
                } else if (!include && set.contains(ts.getTask().getName())) {
                    continue;
                }
            }
            this.doTaskResult(ts, os, names, map);
        }
    }

    private void doTaskResult(TaskResultBase taskResult, List<Object[]> os, Map<String, Long> names, SymbolMap map) {
        Map<StackTrace, Long> samples = taskResult.getSamples();
        long total = 0;
        for (StackTrace s : samples.keySet()) {
            Frame[] frames = s.getFrames();
            String[] fs = new String[frames.length];
            for (int i = frames.length - 1, j = 0; i >= 0; i--, j++) {
                fs[j] = frames[i].toString();
            }
            Object[] o = new Object[3];
            o[0] = map.processSymbols(fs);
            o[1] = samples.get(s);
            o[2] = taskResult.getTask().getName();
            os.add(o);
            total += samples.get(s);
        }
        names.put(taskResult.getTask().getName(), total);
    }

    private static boolean isTaskNameIn(String taskName, List<String> taskList) {
        for (String name : taskList) {
            if (taskName.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private void generateCpuTime(DimensionResult<TaskCPUTime> result, long gcCPUTime, List<Object[]> os,
                                 Map<String, Long> names, SymbolMap map, boolean include, List<String> taskSet) {
        List<TaskCPUTime> list = result.getList();
        for (TaskCPUTime ct : list) {
            if (taskSet != null && !taskSet.isEmpty()) {
                if (include) {
                    if (!isTaskNameIn(ct.getTask().getName(), taskSet)) {
                        continue;
                    }
                } else {
                    if (isTaskNameIn(ct.getTask().getName(), taskSet)) {
                        continue;
                    }
                }
            }

            if (ct instanceof JavaThreadCPUTime jtct) {
                Map<String, Long> vmOperations = jtct.getVmOperations();
                if (vmOperations != null && !vmOperations.isEmpty()) {
                    vmOperations.keySet().forEach(item -> {
                        long time = vmOperations.get(item);
                        StackTrace st = VMOperationUtil.makeStackTrace(item);
                        Frame[] frames = st.getFrames();
                        String[] fs = new String[frames.length];
                        for (int i = frames.length - 1, j = 0; i >= 0; i--, j++) {
                            fs[j] = frames[i].toString();
                        }
                        Object[] o = new Object[3];
                        o[0] = map.processSymbols(fs);
                        o[1] = time;
                        o[2] = ct.getTask().getName();
                        os.add(o);
                    });
                }
            }

            Map<StackTrace, Long> samples = ct.getSamples();
            if (samples != null && !samples.isEmpty()) {
                long taskTotalTime = ct.getUser() + ct.getSystem();
                AtomicLong sampleCount = new AtomicLong();
                samples.values().forEach(sampleCount::addAndGet);
                long perSampleTime = taskTotalTime / sampleCount.get();

                for (StackTrace s : samples.keySet()) {
                    Frame[] frames = s.getFrames();
                    String[] fs = new String[frames.length];
                    for (int i = frames.length - 1, j = 0; i >= 0; i--, j++) {
                        fs[j] = frames[i].toString();
                    }
                    Object[] o = new Object[3];
                    o[0] = map.processSymbols(fs);
                    o[1] = samples.get(s) * perSampleTime;
                    o[2] = ct.getTask().getName();
                    os.add(o);
                }
                names.put(ct.getTask().getName(), taskTotalTime);
            }
        }

        if (gcCPUTime > 0 && (taskSet == null || taskSet.isEmpty() || isTaskNameIn("GC Threads", taskSet))) {
            Object[] o = new Object[3];
            o[0] = map.processSymbols(new String[] {"GC", "JVM"});
            o[1] = gcCPUTime;
            o[2] = "GC Threads";
            os.add(o);
            names.put("GC Threads", gcCPUTime);
        }
    }

    private static class SymbolMap {
        private final Map<String, Integer> map = new HashMap<>();

        String[] processSymbols(String[] fs) {
            if (fs == null || fs.length == 0) {
                return fs;
            }

            String[] result = new String[fs.length];

            synchronized (map) {
                for (int i = 0; i < fs.length; i++) {
                    String symbol = fs[i];
                    int id;
                    if (map.containsKey(symbol)) {
                        id = map.get(symbol);
                    } else {
                        id = map.size() + 1;
                        map.put(symbol, id);
                    }
                    result[i] = String.valueOf(id);
                }
            }

            return result;
        }

        Map<Integer, String> getReverseMap() {
            Map<Integer, String> reverseMap = new HashMap<>();
            map.forEach((key, value) -> reverseMap.put(value, key));
            return reverseMap;
        }
    }

    private static <V> V $(RV<V> rv) {
        try {
            return rv.run();
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    interface RV<V> {
        V run() throws Exception;
    }
}
