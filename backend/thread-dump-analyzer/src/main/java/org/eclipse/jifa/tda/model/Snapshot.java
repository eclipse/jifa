/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.tda.model;

import lombok.Data;
import org.eclipse.jifa.common.Constant;
import org.eclipse.jifa.common.util.CollectionUtil;
import org.eclipse.jifa.tda.enums.MonitorState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class Snapshot {

    // constant pools
    private Pool<String> symbols = new Pool<>();

    private Pool<Frame> frames = new Pool<>();

    private Pool<Trace> traces = new Pool<>();

    private IdentityPool<RawMonitor> rawMonitors= new IdentityPool<>();

    private Pool<Monitor> monitors = new Pool<>();

    private Pool<ConcurrentLock> concurrentLocks = new Pool<>();

    private String path;

    // -1 means unknown
    private long timestamp = -1L;

    private String vmInfo = Constant.UNKNOWN_STRING;

    private final List<JavaThread> javaThreads = new ArrayList<>();

    private final List<Thread> nonJavaThreads = new ArrayList<>();

    private final Map<Integer, Thread> threadMap = new HashMap<>();

    private int nextThreadId = 1;

    private CallSiteTree callSiteTree = new CallSiteTree();

    // -1 means unknown
    private int jniRefs = -1;

    // -1 means unknown
    private int jniWeakRefs = -1;

    private Map<String, List<Thread>> threadGroup;

    private Map<Integer, Map<MonitorState, List<Thread>>> monitorThreads = new HashMap<>();

    // dead locks
    private List<List<JavaThread>> deadLockThreads;

    // parse error
    private final List<Error> errors = new ArrayList<>();

    private static final Pattern GROUP_NAME_PATTERN = Pattern.compile("(?<prefix>.*)[-#]\\d+( .+)?");

    private void assignThreadIdAndComputeThreadGroupInfo() {
        Map<String, List<Thread>> map = new HashMap<>();
        CollectionUtil.forEach(t -> {
            t.setId(nextThreadId++);
            threadMap.put(t.getId(), t);
            String name = t.getName();
            Matcher matcher = GROUP_NAME_PATTERN.matcher(name);
            if (matcher.matches()) {
                String prefix = matcher.group("prefix");
                map.computeIfAbsent(prefix, i -> new ArrayList<>()).add(t);
            }
        }, nonJavaThreads, javaThreads);

        threadGroup = new HashMap<>();
        for (Map.Entry<String, List<Thread>> entry : map.entrySet()) {
            if (entry.getValue().size() > 1) {
                threadGroup.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void post() {
        symbols = null;
        frames = null;
        traces = null;
        monitors = null;
        concurrentLocks = null;

        rawMonitors.freeze();
        callSiteTree.freeze();

        javaThreads.sort(Comparator.comparingInt(Thread::getLineStart));
        nonJavaThreads.sort(Comparator.comparingInt(Thread::getLineStart));
        assignThreadIdAndComputeThreadGroupInfo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Snapshot snapshot = (Snapshot) o;
        return timestamp == snapshot.timestamp && nextThreadId == snapshot.nextThreadId &&
               jniRefs == snapshot.jniRefs &&
               jniWeakRefs == snapshot.jniWeakRefs && Objects.equals(symbols, snapshot.symbols) &&
               Objects.equals(frames, snapshot.frames) && Objects.equals(traces, snapshot.traces) &&
               Objects.equals(rawMonitors, snapshot.rawMonitors) &&
               Objects.equals(monitors, snapshot.monitors) &&
               Objects.equals(concurrentLocks, snapshot.concurrentLocks) &&
               Objects.equals(path, snapshot.path) && Objects.equals(vmInfo, snapshot.vmInfo) &&
               Objects.equals(javaThreads, snapshot.javaThreads) &&
               Objects.equals(nonJavaThreads, snapshot.nonJavaThreads) &&
               Objects.equals(threadMap, snapshot.threadMap) &&
               Objects.equals(callSiteTree, snapshot.callSiteTree) &&
               Objects.equals(threadGroup, snapshot.threadGroup) &&
               Objects.equals(monitorThreads, snapshot.monitorThreads) &&
               Objects.equals(deadLockThreads, snapshot.deadLockThreads) &&
               Objects.equals(errors, snapshot.errors);
    }
}
