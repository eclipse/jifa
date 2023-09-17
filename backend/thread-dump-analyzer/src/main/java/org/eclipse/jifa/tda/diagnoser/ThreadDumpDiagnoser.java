/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.tda.diagnoser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jifa.tda.diagnoser.Diagnostic.Severity;
import org.eclipse.jifa.tda.enums.JavaThreadState;
import org.eclipse.jifa.tda.model.Frame;
import org.eclipse.jifa.tda.model.JavaThread;
import org.eclipse.jifa.tda.model.Snapshot;
import org.eclipse.jifa.tda.model.Thread;
import org.eclipse.jifa.tda.vo.VThread;

/**
 * diagnoses a thread dump based on the given config
 */
public class ThreadDumpDiagnoser {

    private static final String KEY_THREAD_NAME = "name";
    private static final String KEY_THREAD_COUNT = "count";
    private static final String KEY_THRESHOLD = "threshold";

    /**
     * returns a (potentially empty) list of diagnostics based on the given config
     * 
     * @param snapshot the thread dump snapshot
     * @param config   the analyzer config
     * @return list of issues found
     */
    public List<Diagnostic> analyze(Snapshot snapshot, ThreadDumpAnalysisConfig config) {
        List<Diagnostic> results = new ArrayList<>();
        analyzeDeadlock(snapshot, config, results);
        analyzeBlockedThreads(snapshot, config, results);
        analyzeThreadCount(snapshot, config, results);
        analyzeLargeStackSize(snapshot, config, results);
        analyzeCpuRatio(snapshot, config, results);
        analyzeExceptionThread(snapshot, config, results);
        return results;
    }

    private void analyzeDeadlock(Snapshot snapshot, ThreadDumpAnalysisConfig config, List<Diagnostic> results) {
        if (snapshot.getDeadLockThreads() == null) {
            return;
        }
        List<JavaThread> deadlockThreads = new ArrayList<>();
        snapshot.getDeadLockThreads().forEach(deadlockThreads::addAll);
        if (deadlockThreads.size() > 0) {
            results.add(new Diagnostic(Severity.ERROR, Diagnostic.Type.DEADLOCK, createParams(deadlockThreads),
                    toVThread(deadlockThreads)));
        }
    }

    private void analyzeBlockedThreads(Snapshot snapshot, ThreadDumpAnalysisConfig config, List<Diagnostic> results) {
        List<JavaThread> blockedThreads = snapshot.getJavaThreads().stream()
                .filter(t -> t.getJavaThreadState() == JavaThreadState.BLOCKED_ON_MONITOR_ENTER)
                .collect(Collectors.toList());
        if (blockedThreads.size() > config.getHighBlockedThreadsThreshold()) {
            results.add(new Diagnostic(Severity.ERROR, Diagnostic.Type.HIGH_BLOCKED_THREAD_COUNT,
                    createParams(blockedThreads), toVThread(blockedThreads)));
        }
    }

    private void analyzeThreadCount(Snapshot snapshot, ThreadDumpAnalysisConfig config, List<Diagnostic> results) {
        if (config.getHighThreadsThreshold() <= 0) {
            return;
        }
        int count = snapshot.getThreadMap().size();
        if (count >= config.getHighThreadsThreshold()) {
            results.add(new Diagnostic(Severity.WARNING, Diagnostic.Type.HIGH_THREAD_COUNT,
                    createParams(snapshot.getThreadMap().values()), null));
        }
    }

    private void analyzeLargeStackSize(Snapshot snapshot, ThreadDumpAnalysisConfig config, List<Diagnostic> results) {
        if (config.getHighStackSizeThreshold() <= 0)
            return;
        List<JavaThread> threads = snapshot.getJavaThreads().stream()
                .filter(t -> Optional.ofNullable(t.getTrace()).map(trace -> trace.getFrames())
                        .map(frames -> frames.length).orElse(0) > config.getHighStackSizeThreshold())
                .collect(Collectors.toList());
        if (!threads.isEmpty()) {
            Map<String, Object> params = createParams(threads);
            params.put(KEY_THRESHOLD, config.getHighStackSizeThreshold());
            results.add(new Diagnostic(Severity.WARNING, Diagnostic.Type.HIGH_STACK_SIZE, params, toVThread(threads)));
        }
    }

    private void analyzeCpuRatio(Snapshot snapshot, ThreadDumpAnalysisConfig config, List<Diagnostic> results) {
        if (config.getHighCpuConsumedRatio() <= 0)
            return;
        List<JavaThread> threads = snapshot.getJavaThreads().stream().filter(t -> t.getCpu() > 0 && t.getElapsed() > 0)
                .filter(t -> (t.getCpu() / t.getElapsed()) >= config.getHighCpuConsumedRatio())
                .collect(Collectors.toList());
        if (!threads.isEmpty()) {
            results.add(new Diagnostic(Severity.WARNING, Diagnostic.Type.HIGH_CPU_RATIO, createParams(threads),
                    toVThread(threads)));
        }
    }

    private void analyzeExceptionThread(Snapshot snapshot, ThreadDumpAnalysisConfig config, List<Diagnostic> results) {
        if (!config.isReportThrowingException())
            return;
        List<JavaThread> threads = snapshot.getJavaThreads().stream().filter(this::isThrowingException)
                .collect(Collectors.toList());
        if (!threads.isEmpty()) {
            results.add(new Diagnostic(Severity.WARNING, Diagnostic.Type.THREAD_THROWING_EXCEPTION,
                    createParams(threads), toVThread(threads)));
        }
    }

    private boolean isThrowingException(JavaThread thread) {
        if (thread.getTrace() == null || thread.getTrace().getFrames() == null)
            return false;
        String throwableName = Throwable.class.getName();
        Frame[] frames = thread.getTrace().getFrames();
        for (int i = 0; i < Math.min(frames.length, 5); i++) {
            // check at most 5 frames back
            Frame frame = frames[i];
            if (throwableName.equals(frame.getClazz()) && frame.getMethod() != null
                    && frame.getMethod().contains("fillInStackTrace")) {
                return true;
            }
        }
        return false;
    }

    private List<VThread> toVThread(List<? extends Thread> threads) {
        return threads.stream().map(t -> new VThread(t.getId(), t.getName(), null, null)).collect(Collectors.toList());
    }

    private Map<String, Object> createParams(Collection<? extends Thread> threads) {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_THREAD_COUNT, threads.size());

        if (threads.size() == 1) {
            params.put(KEY_THREAD_NAME, threads.stream().findFirst().get().getName());
        }
        return params;
    }
}
