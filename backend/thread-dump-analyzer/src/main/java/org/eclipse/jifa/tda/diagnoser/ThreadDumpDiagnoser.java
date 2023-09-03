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
import java.util.List;
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

    /**
     * returns a (potentially empty) list of diagnostics based on the given config
     * @param snapshot the thread dump snapshot
     * @param config the analyzer config
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
        if(snapshot.getDeadLockThreads() == null) {
            return;
        }
        List<JavaThread> deadlockThreads = new ArrayList<>();
        snapshot.getDeadLockThreads().forEach(deadlockThreads::addAll);
        if(deadlockThreads.size() > 0) {
            String message = String.format("%s threads are in a deadlock", deadlockThreads.size());
            String suggestion = "Deadlocks if two or more threads are waiting for each other indefinetly. They are caused by incorrect ordering of resource locking.";
            results.add(new Diagnostic(Severity.ERROR, Diagnostic.CODE_DEADLOCK, message, suggestion, toVThread(deadlockThreads)));
        }
    }

    private void analyzeBlockedThreads(Snapshot snapshot, ThreadDumpAnalysisConfig config, List<Diagnostic> results) {
        List<JavaThread> blockedThreads = snapshot.getJavaThreads().stream().filter(t -> t.getJavaThreadState()==JavaThreadState.BLOCKED_ON_MONITOR_ENTER).collect(Collectors.toList());
        if(blockedThreads.size() > config.getHighBlockedThreadsThreshold()) {
            String message = blockedThreads.size() == 1 ? "One thread is blocked" : String.format("%s threads are blocked", blockedThreads.size());
            String suggestion = "A large amount of blocked threads often indicate a bottleneck in the software. Examine the stacktraces and check the locking and synchronization.";
            results.add(new Diagnostic(Severity.ERROR, Diagnostic.CODE_HIGH_BLOCKED_THREAD_COUNT, message, suggestion, toVThread(blockedThreads)));
        }
    }


    private void analyzeThreadCount(Snapshot snapshot, ThreadDumpAnalysisConfig config, List<Diagnostic> results) {
        if(config.getHighThreadsThreshold()<=0) {
            return;
        }
        int count = snapshot.getThreadMap().size();
        if(count>=config.getHighThreadsThreshold()) {
            String message = String.format("%s is a high thread count", count);
            String suggestion = "Such high thread counts can lead to memory exhaustion and thread starvation. Look for thread leaks or consider using ThreadPools to reduce thread creation and limit their amount.";
            results.add(new Diagnostic(Severity.WARNING, Diagnostic.CODE_HIGH_THREAD_COUNT, message, suggestion, null));
        }
    }

    private void analyzeLargeStackSize(Snapshot snapshot, ThreadDumpAnalysisConfig config, List<Diagnostic> results) {
        if(config.getHighStackSizeThreshold()<=0)
            return;
        List<JavaThread> threads = snapshot.getJavaThreads().stream().filter(t-> Optional.ofNullable(t.getTrace()).map(trace -> trace.getFrames()).map(frames -> frames.length).orElse(0) > config.getHighStackSizeThreshold()).collect(Collectors.toList());
        if(!threads.isEmpty()) {
            String message = threads.size() == 1 ? String.format("%s has a very large stack", threads.get(0).getName()) : String.format("%s threads have a very large stack size (> %s)", threads.size(), config.getHighStackSizeThreshold());
            String suggestion = "Large stack sizes can lead to stack overflow errors and decrease performance. Check for too deep recursions";
            results.add(new Diagnostic(Severity.WARNING, Diagnostic.CODE_HIGH_STACK_SIZE, message, suggestion, toVThread(threads)));
        }
    }

    private void analyzeCpuRatio(Snapshot snapshot, ThreadDumpAnalysisConfig config, List<Diagnostic> results) {
        if(config.getHighCpuConsumedRatio()<=0)
            return;
        List<JavaThread> threads = snapshot.getJavaThreads().stream().filter(t-> t.getCpu()>0 && t.getElapsed()>0).filter(t -> (t.getCpu()/t.getElapsed()) >= config.getHighCpuConsumedRatio()).collect(Collectors.toList());
        if(!threads.isEmpty()) {
            String message = threads.size() == 1 ? String.format("%s has a high CPU ratio", threads.get(0).getName()) : String.format("%s threads have a high CPU ratio", threads.size());
            String suggestion = "If the CPU ratio of a thread is high, it is consuming large amounts of CPU over the lifetime of the thread. That is not necessarily bad, but marks very active threads.";
            results.add(new Diagnostic(Severity.WARNING, Diagnostic.CODE_HIGH_CPU_RATIO, message, suggestion, toVThread(threads)));
        }
    }

    private void analyzeExceptionThread(Snapshot snapshot, ThreadDumpAnalysisConfig config, List<Diagnostic> results) {
        if(!config.isReportThrowingException())
            return;
        List<JavaThread> threads = snapshot.getJavaThreads().stream().filter(this::isThrowingException).collect(Collectors.toList());
        if(!threads.isEmpty()) {
            String message = threads.size() == 1 ? String.format("%s is throwing an exception", threads.get(0).getName()) : String.format("%s threads are throwing an exception", threads.size());
            String suggestion = "A thread throwing an exception may indicate an issue in the application. Keep in mind that creating stack traces can be expensive, so if an application often throws exceptions, it can impace performance.";
            results.add(new Diagnostic(Severity.WARNING, Diagnostic.CODE_THREAD_THROWING_EXCEPTION, message, suggestion, toVThread(threads)));
        }
    }

    private boolean isThrowingException(JavaThread thread) {
        if(thread.getTrace()==null || thread.getTrace().getFrames()==null)
            return false;
        String throwableName = Throwable.class.getName();
        Frame[] frames = thread.getTrace().getFrames();
        for (int i = 0; i < Math.min(frames.length,5); i++) {
            //check at most 5 frames back
            Frame frame = frames[i];
            if(throwableName.equals(frame.getClazz()) && frame.getMethod()!=null && frame.getMethod().contains("fillInStackTrace")) {
                return true;
            }
        }
        return false;
    }

    private List<VThread> toVThread(List<? extends Thread> threads) {
        return threads.stream().map(t -> new VThread(t.getId(), t.getName(), null, null)).collect(Collectors.toList());
    }

}
