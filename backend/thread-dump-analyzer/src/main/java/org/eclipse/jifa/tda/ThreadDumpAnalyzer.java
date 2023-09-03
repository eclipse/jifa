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

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jifa.common.cache.Cacheable;
import org.eclipse.jifa.common.cache.ProxyBuilder;
import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.CollectionUtil;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.tda.diagnoser.Diagnostic;
import org.eclipse.jifa.tda.diagnoser.ThreadDumpAnalysisConfig;
import org.eclipse.jifa.tda.diagnoser.ThreadDumpDiagnoser;
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
import org.eclipse.jifa.tda.vo.VBlockingThread;
import org.eclipse.jifa.tda.vo.VFrame;
import org.eclipse.jifa.tda.vo.VMonitor;
import org.eclipse.jifa.tda.vo.VSearchResult;
import org.eclipse.jifa.tda.vo.VThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread dump analyzer
 */
public class ThreadDumpAnalyzer {

    private final Snapshot snapshot;
    private final Logger LOGGER = LoggerFactory.getLogger(ThreadDumpAnalyzer.class);

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
        return PageViewBuilder.build(threads, paging, this::convertToVThread);
    }

    private VThread convertToVThread(Thread thread) {
        VThread vThread = new VThread();
        vThread.setId(thread.getId());
        vThread.setName(thread.getName());
        if(thread.getElapsed()>0) {
            vThread.setElapsed(thread.getElapsed());
        }
        if(thread.getCpu()>0) {
            vThread.setCpu(thread.getCpu());
        }
        return vThread;
    }

    /**
     * @param name   the thread name
     * @param type   the thread type
     * @param threadState the thread state
     * @param ids list of thread ids
     * @param paging paging request
     * @return the threads filtered by name, state, type and id
     */
    public PageView<VThread> threads(String name, ThreadType type, String threadState, List<Integer> ids, PagingRequest paging) {
        List<Thread> threads = new ArrayList<>();
        CollectionUtil.forEach(t -> {
            if (type != null && t.getType() != type) {
                return;
            }
            if (StringUtils.isNotBlank(name) && !t.getName().contains(name)) {
                return;
            }
            if (StringUtils.isNotBlank(threadState) && !getThreadState(t).equals(threadState) ) {
                return;
            }
            if(ids != null && !ids.isEmpty() && !ids.contains(t.getId())) {
                return;
            }
            threads.add(t);
        }, snapshot.getJavaThreads(), snapshot.getNonJavaThreads());

        return buildVThreadPageView(threads, paging);
    }

    private String getThreadState(Thread t) {
        if (t instanceof JavaThread) {
            JavaThread jt = (JavaThread)t;
            if (jt.getJavaThreadState() != null)
                return String.valueOf(jt.getJavaThreadState());
        }
        return String.valueOf(t.getOsThreadState());
    }

 /**
     * @param id   the thread id
     * @return the thread
     */
    public VThread thread(int id) {
        Thread thread = snapshot.getThreadMap().get(id);
        if (thread == null) {
            throw new IllegalArgumentException("Thread id is illegal: " + id);
        }
        VThread vThread = new VThread();
        vThread.setId(thread.getId());
        vThread.setName(thread.getName());
        return vThread;
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

    /**
     * 
     * @return the threads that block one or more other threads
     */
    public List<VBlockingThread> blockingThreads() {
       
        List<VBlockingThread> result = new ArrayList<>();
        
        Map<Integer,Map<MonitorState, List<Thread>>> allMonitors = snapshot.getMonitorThreads();
        for (Entry<Integer,Map<MonitorState, List<Thread>>> monitorEntry : allMonitors.entrySet()) {
            Map<MonitorState, List<Thread>> monitorMap = monitorEntry.getValue();
            if(!monitorMap.keySet().contains(MonitorState.LOCKED))
                continue;
            Thread blockingThread = monitorMap.get(MonitorState.LOCKED).stream().findFirst().orElse(null);
            List<Thread> blockedThreads = new ArrayList<>();
            if(monitorMap.keySet().contains(MonitorState.WAITING_TO_LOCK))
                blockedThreads.addAll(monitorMap.get(MonitorState.WAITING_TO_LOCK));
            if(monitorMap.keySet().contains(MonitorState.WAITING_TO_RE_LOCK))
                blockedThreads.addAll(monitorMap.get(MonitorState.WAITING_TO_RE_LOCK));
            if(!blockedThreads.isEmpty() && blockingThread!=null)
            {
                VBlockingThread r = new VBlockingThread();
                r.setBlockedThreads(blockedThreads.stream().map(this::convertToVThread).collect(Collectors.toList()));
                r.setBlockingThread(convertToVThread(blockingThread));
                Monitor mon = findBlockingMonitor(blockedThreads.get(0));
                if(mon!=null) {
                    r.setHeldLock(new VMonitor(mon.getRawMonitor().getId(), mon.getRawMonitor().getAddress(),mon.getRawMonitor().isClassInstance(), mon.getRawMonitor().getClazz(), mon.getState()));
                }
                result.add(r);
            }
           
        }
        Comparator<VBlockingThread> comparator = Comparator.<VBlockingThread>comparingInt(m ->  m.getBlockedThreads().size()).reversed().thenComparing(m -> m.getBlockingThread().getName());
        result.sort(comparator);
        return result;
    }

    /**
     * creates a list of the top most CPU consuming threads
     * @param type only include threads of the given type. All threads will be considered if type is <code>null</code>
     * @param max the max amount of threads to return. If max is <code>-1</code> all threads will be returned
     * @return list of CPU consuming threads from most expensive, to least expensive
     */
    public List<VThread> cpuConsumingThreads(ThreadType type, int max) {
        Stream<Thread> stream = snapshot.getThreadMap().values().stream().filter(t -> type == null || t.getType()==type);
        stream = stream.sorted(Comparator.comparingDouble(Thread::getCpu).reversed()).limit(max < 0 ? Integer.MAX_VALUE : max);
        return stream.map(this::convertToVThread).collect(Collectors.toList());
    } 

    /**
     * computes which threads consumed the most CPU between 2 thread dumps
     * @param other the other (later) thread dump result
     * @param max the maximum amount of threads to return (-1 for unlimited)
     * @return a list of the top cpu consuming threads between both dumps
     */
    public List<VThread> cpuConsumingThreadsCompare(ThreadDumpAnalyzer other, int max, ThreadType type) {
        max = max < 0 ? Integer.MAX_VALUE : max;
        Map<Thread, Double> cpuConsumingThreads = new HashMap<>();
        for(Thread first : snapshot.getThreadMap().values()) {
            if(type!=null && first.getType()!=type) {
                continue;
            }
            Thread second = other.snapshot.getThreadMap().values().stream().filter(t -> t.getTid()==first.getTid()).findFirst().orElse(null);
            if(second != null && second.getCpu()>0 ) {
                cpuConsumingThreads.put(first, second.getCpu() - first.getCpu());
            }
        }
        List<VThread> result = new ArrayList<VThread>();
        cpuConsumingThreads.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(max).forEach(e -> {
            Thread thread = e.getKey();
            VThread vthread = convertToVThread(thread);
            vthread.setCpu(e.getValue());
            result.add(vthread);
        });
        return result;
    }

    /**
     * analyzes the thread dump for potential issues based on the given configuration and returns any issues found
     * 
     * @param config the config to use for the analyzer
     * @return potentially empty list of diagnostic issues found
     */
    public List<Diagnostic> diagnose(ThreadDumpAnalysisConfig config) {
        return new ThreadDumpDiagnoser().analyze(snapshot, config);
    }

    private Monitor findBlockingMonitor(Thread thread) 
    {
        List<Monitor> candidates = new ArrayList<>();
        if(thread instanceof JavaThread)
        {
            JavaThread blockedThread = (JavaThread)thread;
            if(blockedThread.getMonitors()!=null)
                candidates.addAll(blockedThread.getMonitors()); //not sure if this is needed
            Frame[] frames = blockedThread.getTrace().getFrames();
            for (Frame frame : frames) {
                if(frame.getMonitors()!=null)
                {
                    Arrays.stream(frame.getMonitors()).forEach(candidates::add);
                    //only need the first frame really
                    break;
                }
            }
        }
        return candidates.stream().filter(m -> m.getState()==MonitorState.WAITING_TO_LOCK || m.getState()==MonitorState.WAITING_TO_RE_LOCK).findFirst().orElse(null);
    }  

    /**
     * searches threads based on the given criteria. See SearchQuery for
     * details on how to create the predicate 
     * @param searchFilter
     * @return list of search results
     */
    public List<VSearchResult> search(Predicate<Thread> searchFilter) {
        return snapshot.getThreadMap().values().stream().filter(searchFilter).map(t -> {
            VSearchResult result = new VSearchResult();
            result.setCpu(t.getCpu());
            result.setElapsed(t.getElapsed());
            result.setId(t.getId());
            result.setOsState(t.getOsThreadState());
            if(t instanceof JavaThread) {
                result.setJavaState((((JavaThread)t).getJavaThreadState()));
            } 
            result.setName(t.getName());
            result.setFilename(Path.of((snapshot.getPath())).getFileName().toString());
            try {
                result.setLines(rawContentOfThread(t.getId()));
            }
            catch(IOException e) {
                LOGGER.error("Failed to load raw content of thread {}",t.getName(), e);
                result.setLines(Arrays.asList("Failed to load"));
            }
            return result;
        }).collect(Collectors.toList());
    }

}
