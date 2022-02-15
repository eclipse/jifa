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

package org.eclipse.jifa.tda;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jifa.common.cache.Cacheable;
import org.eclipse.jifa.common.cache.ProxyBuilder;
import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.CollectionUtil;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.tda.enums.MonitorState;
import org.eclipse.jifa.tda.enums.ThreadType;
import org.eclipse.jifa.tda.model.CallSiteTree;
import org.eclipse.jifa.tda.model.Frame;
import org.eclipse.jifa.tda.model.IdentityPool;
import org.eclipse.jifa.tda.model.JavaThread;
import org.eclipse.jifa.tda.model.Monitor;
import org.eclipse.jifa.tda.model.RawMonitor;
import org.eclipse.jifa.tda.model.Snapshot;
import org.eclipse.jifa.tda.model.Thread;
import org.eclipse.jifa.tda.parser.ParserFactory;
import org.eclipse.jifa.tda.vo.Content;
import org.eclipse.jifa.tda.vo.Overview;
import org.eclipse.jifa.tda.vo.VFrame;
import org.eclipse.jifa.tda.vo.VMonitor;
import org.eclipse.jifa.tda.vo.VThread;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread dump analyzer
 */
public class ThreadDumpAnalyzer {

    private final Snapshot snapshot;

    ThreadDumpAnalyzer(Path path, ProgressListener listener) {
        snapshot = ParserFactory.buildParser(path).parse(path, listener);
    }

    /**
     * build a parser for a thread dump
     *
     * @param path     the path of thread dump
     * @param listener progress listener
     * @return analyzer
     */
    public static ThreadDumpAnalyzer build(Path path, ProgressListener listener) {
        return ProxyBuilder.build(ThreadDumpAnalyzer.class,
                                  new Class[]{Path.class, ProgressListener.class},
                                  new Object[]{path, listener});
    }

    private void computeThreadState(Overview o, Thread thread) {
        ThreadType type = thread.getType();
        switch (type) {
            case JAVA:
                JavaThread jt = ((JavaThread) thread);
                o.getJavaThreadStat().inc(jt.getJavaThreadState());
                o.getJavaThreadStat().inc(jt.getOsThreadState());
                if (jt.isDaemon()) {
                    o.getJavaThreadStat().incDaemon();
                }
                break;
            case JIT:
                o.getJitThreadStat().inc(thread.getOsThreadState());
                break;
            case GC:
                o.getGcThreadStat().inc(thread.getOsThreadState());
                break;
            case VM:
                o.getOtherThreadStat().inc(thread.getOsThreadState());
                break;
        }
        o.getThreadStat().inc(thread.getOsThreadState());
    }

    /**
     * @return the overview of the thread dump
     */
    @Cacheable
    public Overview overview() {
        Overview o = new Overview();
        CollectionUtil.forEach(t -> computeThreadState(o, t), snapshot.getJavaThreads(), snapshot.getNonJavaThreads());

        snapshot.getThreadGroup().forEach(
            (p, l) -> {
                for (Thread t : l) {
                    o.getThreadGroupStat().computeIfAbsent(p, i -> new Overview.ThreadStat()).inc(t.getOsThreadState());
                }
            }
        );
        o.setTimestamp(snapshot.getTimestamp());
        o.setVmInfo(snapshot.getVmInfo());
        o.setJniRefs(snapshot.getJniRefs());
        o.setJniWeakRefs(snapshot.getJniWeakRefs());

        if (snapshot.getDeadLockThreads() != null) {
            o.setDeadLockCount(snapshot.getDeadLockThreads().size());
        }

        o.setErrorCount(snapshot.getErrors().size());
        return o;
    }

    /**
     * @return the call site tree by parent id
     */
    public PageView<VFrame> callSiteTree(int parentId, PagingRequest paging) {
        CallSiteTree tree = snapshot.getCallSiteTree();
        if (parentId < 0 || parentId >= tree.getId2Node().length) {
            throw new IllegalArgumentException("Illegal parent id: " + parentId);
        }
        CallSiteTree.Node node = tree.getId2Node()[parentId];
        List<CallSiteTree.Node> children = node.getChildren() != null ? node.getChildren() : Collections.emptyList();
        return PageViewBuilder.build(children, paging, n -> {
            VFrame vFrame = new VFrame();
            vFrame.setId(n.getId());
            vFrame.setWeight(n.getWeight());
            vFrame.setEnd(n.getChildren() == null);

            Frame frame = n.getFrame();
            vFrame.setClazz(frame.getClazz());
            vFrame.setMethod(frame.getMethod());
            vFrame.setModule(frame.getModule());
            vFrame.setSourceType(frame.getSourceType());
            vFrame.setSource(frame.getSource());

            vFrame.setLine(frame.getLine());

            if (frame.getMonitors() != null) {
                List<VMonitor> vMonitors = new ArrayList<>();
                for (Monitor monitor : frame.getMonitors()) {
                    String clazz = null;
                    RawMonitor rm = monitor.getRawMonitor();
                    clazz = rm.getClazz();
                    vMonitors.add(new VMonitor(rm.getId(), rm.getAddress(), rm.isClassInstance(),
                                               clazz,
                                               monitor.getState()));
                }
                vFrame.setMonitors(vMonitors);
            }
            return vFrame;
        });
    }

    private PageView<VThread> buildVThreadPageView(List<Thread> threads, PagingRequest paging) {
        return PageViewBuilder.build(threads, paging, thread -> {
            VThread vThread = new VThread();
            vThread.setId(thread.getId());
            vThread.setName(thread.getName());
            return vThread;
        });
    }

    /**
     * @param name   the thread name
     * @param type   the thread type
     * @param paging paging request
     * @return the threads filtered by name and type
     */
    public PageView<VThread> threads(String name, ThreadType type, PagingRequest paging) {
        List<Thread> threads = new ArrayList<>();
        CollectionUtil.forEach(t -> {
            if (type != null && t.getType() != type) {
                return;
            }
            if (StringUtils.isNotBlank(name) && !t.getName().contains(name)) {
                return;
            }
            threads.add(t);
        }, snapshot.getJavaThreads(), snapshot.getNonJavaThreads());

        return buildVThreadPageView(threads, paging);
    }

    /**
     * @param groupName the thread group name
     * @param paging    paging request
     * @return the threads filtered by group name and type
     */
    public PageView<VThread> threadsOfGroup(String groupName, PagingRequest paging) {
        List<Thread> threads = snapshot.getThreadGroup().getOrDefault(groupName, Collections.emptyList());
        return buildVThreadPageView(threads, paging);
    }

    public List<String> rawContentOfThread(int id) throws IOException {
        Thread thread = snapshot.getThreadMap().get(id);
        if (thread == null) {
            throw new IllegalArgumentException("Thread id is illegal: " + id);
        }
        String path = snapshot.getPath();

        int start = thread.getLineStart();
        int end = thread.getLineEnd();
        List<String> content = new ArrayList<>();

        try (LineNumberReader lnr = new LineNumberReader(new FileReader(path))) {
            for (int i = 1; i < start; i++) {
                lnr.readLine();
            }

            for (int i = start; i <= end; i++) {
                content.add(lnr.readLine());
            }
        }

        return content;
    }

    /**
     * @param lineNo    start line number
     * @param lineLimit line count
     * @return the raw content
     * @throws IOException
     */
    public Content content(int lineNo, int lineLimit) throws IOException {
        String path = snapshot.getPath();

        int end = lineNo + lineLimit - 1;
        List<String> content = new ArrayList<>();
        boolean reachEnd;

        try (LineNumberReader lnr = new LineNumberReader(new FileReader(path))) {
            for (int i = 1; i < lineNo; i++) {
                String line = lnr.readLine();
                if (line == null) {
                    break;
                }
            }

            for (int i = lineNo; i <= end; i++) {
                String line = lnr.readLine();
                if (line == null) {
                    break;
                }
                content.add(line);
            }

            String line = lnr.readLine();
            reachEnd = line == null;
        }
        return new Content(content, reachEnd);
    }

    /**
     * @param paging paging request
     * @return the monitors
     */
    public PageView<VMonitor> monitors(PagingRequest paging) {
        IdentityPool<RawMonitor> monitors = snapshot.getRawMonitors();
        return PageViewBuilder.build(monitors.objects(), paging,
                                     m -> new VMonitor(m.getId(), m.getAddress(), m.isClassInstance(), m.getClazz()));
    }

    /**
     * @param id     monitor id
     * @param state  monitor state
     * @param paging paging request
     * @return the threads by monitor id and state
     */
    public PageView<VThread> threadsByMonitor(int id, MonitorState state, PagingRequest paging) {
        Map<MonitorState, List<Thread>> map = snapshot.getMonitorThreads().get(id);
        if (map == null) {
            throw new IllegalArgumentException("Illegal monitor id: " + id);
        }
        return buildVThreadPageView(map.getOrDefault(state, Collections.emptyList()), paging);
    }

    /**
     * @param id monitor id
     * @return the <state, count> map by monitor id
     */
    public Map<MonitorState, Integer> threadCountsByMonitor(int id) {
        Map<MonitorState, List<Thread>> map = snapshot.getMonitorThreads().get(id);
        if (map == null) {
            throw new IllegalArgumentException("Illegal monitor id: " + id);
        }

        Map<MonitorState, Integer> counts = new HashMap<>();
        map.forEach((s, l) -> counts.put(s, l.size()));
        return counts;
    }
}
