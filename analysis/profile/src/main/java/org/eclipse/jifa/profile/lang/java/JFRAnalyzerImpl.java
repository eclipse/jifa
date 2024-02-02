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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.eclipse.jifa.profile.lang.java.common.ProfileDimension;
import org.eclipse.jifa.profile.lang.java.model.jfr.RecordedEvent;
import org.eclipse.jifa.profile.lang.java.model.AnalysisResult;
import org.eclipse.jifa.profile.lang.java.request.AnalysisRequest;
import org.eclipse.jifa.profile.lang.java.request.DimensionBuilder;
import org.eclipse.jifa.profile.lang.java.extractor.*;
import org.eclipse.jifa.profile.model.Problem;
import org.eclipse.jifa.profile.exception.ProfileAnalysisException;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.Severity;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.stream.Collectors;

@Slf4j
public class JFRAnalyzerImpl implements JFRAnalyzer {

    private JFRAnalysisContext context;

    private ProgressListener listener;

    @Override
    public AnalysisResult execute(AnalysisRequest request, ProgressListener listener) throws ProfileAnalysisException {
        this.context = new JFRAnalysisContext(request);
        this.listener = listener;
        try {
            return analyze(request);
        } catch (Exception e) {
            if (e instanceof ProfileAnalysisException) {
                throw (ProfileAnalysisException) e;
            }
            throw new ProfileAnalysisException(e);
        }
    }

    private AnalysisResult analyze(AnalysisRequest request) throws Exception {
        listener.beginTask("Analyzing", 5);
        long startTime = System.currentTimeMillis();
        AnalysisResult r = new AnalysisResult();

        IItemCollection collection = this.loadEvents(request);

        this.analyzeProblemsIfNeeded(request, collection, r);

        this.transformEvents(request, collection);

        this.sortEvents();

        this.processEvents(request, r);

        r.setProcessingTimeMillis(System.currentTimeMillis() - startTime);
        log.info(String.format("Analysis took %d milliseconds", r.getProcessingTimeMillis()));

        return r;
    }

    private void processEvents(AnalysisRequest request, AnalysisResult r) throws Exception {
        listener.subTask("Do Extractors");
        List<RecordedEvent> events = this.context.getEvents();
        final List<Extractor> extractors = getExtractors(request);

        if (request.getParallelWorkers() > 1) {
            CountDownLatch countDownLatch = new CountDownLatch(extractors.size());
            ExecutorService es = Executors.newFixedThreadPool(request.getParallelWorkers());
            extractors.forEach(item -> es.submit(() -> {
                try {
                    doExtractorWork(events, item, r);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                }
            }));
            countDownLatch.await();
            es.shutdown();
        } else {
            extractors.forEach(item -> {
                doExtractorWork(events, item, r);
            });
        }
        listener.worked(1);
    }

    private void doExtractorWork(List<RecordedEvent> events, Extractor extractor, AnalysisResult r) {
        events.forEach(extractor::process);
        extractor.fillResult(r);
    }

    private List<Extractor> getExtractors(AnalysisRequest request) {
        return getExtractors(request.getDimensions());
    }

    private void sortEvents() {
        listener.subTask("Sort Events");
        this.context.getEvents().sort(Comparator.comparing(RecordedEvent::getStartTime));
        listener.worked(1);
    }

    private void transformEvents(AnalysisRequest request, IItemCollection collection) throws Exception {
        listener.subTask("Transform Events");
        List<IItem> list = collection.stream().flatMap(IItemIterable::stream).collect(Collectors.toList());

        if (request.getParallelWorkers() > 1) {
            parseEventsParallel(list, request.getParallelWorkers());
        } else {
            list.forEach(this::parseEventItem);
        }

        listener.worked(1);
    }

    private IItemCollection loadEvents(AnalysisRequest request) throws Exception {
        try {
            listener.subTask("Load Events");
            if (request.getInput() != null) {
                return JfrLoaderToolkit.loadEvents(request.getInput().toFile());
            } else {
                return JfrLoaderToolkit.loadEvents(request.getInputStream());
            }
        } finally {
            listener.worked(1);
        }
    }

    private void analyzeProblemsIfNeeded(AnalysisRequest request, IItemCollection collection, AnalysisResult r) {
        listener.subTask("Analyze Problems");
        if ((request.getDimensions() & ProfileDimension.PROBLEMS.getValue()) != 0) {
            this.analyzeProblems(collection, r);
        }
        listener.worked(1);
    }

    private void parseEventsParallel(List<IItem> list, int workers) throws Exception {
        listener.subTask("Transform Events");
        CountDownLatch countDownLatch = new CountDownLatch(list.size());
        ExecutorService es = Executors.newFixedThreadPool(workers);
        list.forEach(item -> es.submit(() -> {
            try {
                this.parseEventItem(item);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                countDownLatch.countDown();
            }
        }));
        countDownLatch.await();
        es.shutdown();
        listener.worked(workers);
    }

    private void analyzeProblems(IItemCollection collection, AnalysisResult r) {
        r.setProblems(new ArrayList<>());
        for (IRule rule : RuleRegistry.getRules()) {
            RunnableFuture<IResult> future;
            try {
                future = rule.createEvaluation(collection, IPreferenceValueProvider.DEFAULT_VALUES, null);
                future.run();
                IResult result = future.get();
                Severity severity = result.getSeverity();
                if (severity == Severity.WARNING) {
                    r.getProblems().add(new Problem(result.getSummary(), result.getSolution()));
                }
            } catch (Throwable t) {
                log.error("Failed to run jmc rule {}", rule.getName());
            }
        }
    }

    private void parseEventItem(IItem item) {
        RecordedEvent event = RecordedEvent.newInstance(item, this.context.getSymbols());

        synchronized (this.context.getEvents()) {
            this.context.addEvent(event);
            if (event.getSettingFor() != null) {
                RecordedEvent.SettingFor sf = event.getSettingFor();
                this.context.putEventTypeId(sf.getEventType(), sf.getEventId());
            }
        }
    }

    private List<Extractor> getExtractors(int dimensions) {
        List<Extractor> extractors = new ArrayList<>();
        Map<Integer, Extractor> extractorMap = new HashMap<>() {
            {
                put(DimensionBuilder.CPU, new CPUTimeExtractor(context));
                put(DimensionBuilder.CPU_SAMPLE, new CPUSampleExtractor(context));
                put(DimensionBuilder.WALL_CLOCK, new WallClockExtractor(context));
                put(DimensionBuilder.NATIVE_EXECUTION_SAMPLES, new NativeExecutionExtractor(context));
                put(DimensionBuilder.ALLOC, new AllocationsExtractor(context));
                put(DimensionBuilder.MEM, new AllocatedMemoryExtractor(context));

                put(DimensionBuilder.FILE_IO_TIME, new FileIOTimeExtractor(context));
                put(DimensionBuilder.FILE_READ_SIZE, new FileReadExtractor(context));
                put(DimensionBuilder.FILE_WRITE_SIZE, new FileWriteExtractor(context));

                put(DimensionBuilder.SOCKET_READ_TIME, new SocketReadTimeExtractor(context));
                put(DimensionBuilder.SOCKET_READ_SIZE, new SocketReadSizeExtractor(context));
                put(DimensionBuilder.SOCKET_WRITE_TIME, new SocketWriteTimeExtractor(context));
                put(DimensionBuilder.SOCKET_WRITE_SIZE, new SocketWriteSizeExtractor(context));

                put(DimensionBuilder.LOCK_WAIT_TIME, new LockWaitTimeExtractor(context));
                put(DimensionBuilder.LOCK_ACQUIRE, new LockAcquireExtractor(context));
                put(DimensionBuilder.SYNCHRONIZATION, new SynchronizationExtractor(context));

                put(DimensionBuilder.CLASS_LOAD_COUNT, new ClassLoadCountExtractor(context));
                put(DimensionBuilder.CLASS_LOAD_WALL_TIME, new ClassLoadWallTimeExtractor(context));

                put(DimensionBuilder.THREAD_SLEEP, new ThreadSleepTimeExtractor(context));
            }
        };

        extractorMap.keySet().forEach(item -> {
            if ((dimensions & item) != 0) {
                extractors.add(extractorMap.get(item));
            }
        });

        return extractors;
    }
}
