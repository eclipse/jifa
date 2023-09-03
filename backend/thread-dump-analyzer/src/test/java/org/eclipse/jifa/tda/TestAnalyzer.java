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

import static org.junit.Assert.assertEquals;

import java.util.EnumSet;
import java.util.List;

import org.eclipse.jifa.common.listener.DefaultProgressListener;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.tda.diagnoser.Diagnostic;
import org.eclipse.jifa.tda.diagnoser.ThreadDumpAnalysisConfig;
import org.eclipse.jifa.tda.enums.JavaThreadState;
import org.eclipse.jifa.tda.enums.OSTreadState;
import org.eclipse.jifa.tda.enums.ThreadType;
import org.eclipse.jifa.tda.util.SearchQuery;
import org.eclipse.jifa.tda.vo.Content;
import org.eclipse.jifa.tda.vo.Overview;
import org.eclipse.jifa.tda.vo.VBlockingThread;
import org.eclipse.jifa.tda.vo.VFrame;
import org.eclipse.jifa.tda.vo.VMonitor;
import org.eclipse.jifa.tda.vo.VSearchResult;
import org.eclipse.jifa.tda.vo.VThread;
import org.junit.Assert;
import org.junit.Test;

public class TestAnalyzer extends TestBase {

    @Test
    public void test() throws Exception {
        ThreadDumpAnalyzer tda =
            new ThreadDumpAnalyzer(pathOfResource("jstack_8.log"), new DefaultProgressListener());
        Overview o1 = tda.overview();
        Overview o2 = tda.overview();
        Assert.assertEquals(o1, o2);
        Assert.assertEquals(o1.hashCode(), o2.hashCode());

        PageView<VThread> threads = tda.threads("main", ThreadType.JAVA, null, new PagingRequest(1, 1));
        Assert.assertEquals(1, threads.getTotalSize());

        PageView<VFrame> frames = tda.callSiteTree(0, new PagingRequest(1, 16));
        Assert.assertTrue(frames.getTotalSize() > 0);
        Assert.assertNotEquals(frames.getData().get(0), frames.getData().get(1));

        PageView<VMonitor> monitors = tda.monitors(new PagingRequest(1, 8));
        Assert.assertTrue(monitors.getTotalSize() > 0);

        Content line2 = tda.content(2, 1);
        Assert.assertEquals("Full thread dump OpenJDK 64-Bit Server VM (18-internal+0-adhoc.denghuiddh.my-jdk mixed " +
                            "mode, sharing):", line2.getContent().get(0));
    }

    @Test
    public void testFilter() throws Exception {
        ThreadDumpAnalyzer tda =
            new ThreadDumpAnalyzer(pathOfResource("jstack_8.log"), new DefaultProgressListener());
        Overview o1 = tda.overview();
        Overview o2 = tda.overview();
        Assert.assertEquals(o1, o2);
        Assert.assertEquals(o1.hashCode(), o2.hashCode());

        PageView<VThread> threads = tda.threads(null, null, JavaThreadState.RUNNABLE.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(18, threads.getTotalSize());

        threads = tda.threads(null, ThreadType.GC, JavaThreadState.RUNNABLE.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(10, threads.getTotalSize());

        threads = tda.threads(null, ThreadType.JAVA, JavaThreadState.RUNNABLE.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(3, threads.getTotalSize());

        threads = tda.threads(null, ThreadType.JAVA, JavaThreadState.IN_OBJECT_WAIT.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(2, threads.getTotalSize());

        threads = tda.threads(null, null, OSTreadState.OBJECT_WAIT.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(0, threads.getTotalSize());

        threads = tda.threads("Refer", ThreadType.JAVA, JavaThreadState.IN_OBJECT_WAIT.toString(), new PagingRequest(1, 100));
        Assert.assertEquals(1, threads.getTotalSize());
    }

    @Test
    public void testBlockingThreads() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_large_with_blocked.log"), new DefaultProgressListener());
        List<VBlockingThread> blockingThreads = tda.blockingThreads();
        assertEquals(4,blockingThreads.size());

        VBlockingThread t = blockingThreads.get(0);
        assertEquals("Thread-7728157", t.getBlockingThread().getName());
        assertEquals(19, t.getBlockedThreads().size());
        assertEquals("Thread-7716893", t.getBlockedThreads().get(0).getName());
        assertEquals(0x00000001d3af89c0L, t.getHeldLock().getAddress());
        assertEquals("org.example.jmsadapter.listener.CrossInstanceLock", t.getHeldLock().getClazz());

        t = blockingThreads.get(1);
        assertEquals("Thread-7728079", t.getBlockingThread().getName());
        assertEquals(6, t.getBlockedThreads().size());
        assertEquals("Thread-7728084", t.getBlockedThreads().get(0).getName());
        assertEquals(0x00000001d3b1f158L, t.getHeldLock().getAddress());
        assertEquals("org.example.jmsadapter.listener.CrossInstanceLock", t.getHeldLock().getClazz());
    }

    @Test
    public void testCpuConsumingThreads() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_with_deadlocks.log"), new DefaultProgressListener());
        List<VThread> cpuConsumingThreads = tda.cpuConsumingThreads(null, 5);
        assertEquals(5,cpuConsumingThreads.size());
        
        VThread t = cpuConsumingThreads.get(0);
        assertEquals("main", t.getName());
        assertEquals(108.49, t.getCpu(),0.1);

        t = cpuConsumingThreads.get(1);
        assertEquals("C1 CompilerThread0", t.getName());
        assertEquals(19.04, t.getCpu(),0.1);
    }

    @Test
    public void testCpuConsumingThreadsWithFilter() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_with_deadlocks.log"), new DefaultProgressListener());
        List<VThread> cpuConsumingThreads = tda.cpuConsumingThreads(ThreadType.JAVA, 50);
        assertEquals(9,cpuConsumingThreads.size());

        VThread t = cpuConsumingThreads.get(0);
        assertEquals("main", t.getName());
        assertEquals(108.49, t.getCpu(),0.1);

        t = cpuConsumingThreads.get(1);
        assertEquals("Sweeper thread", t.getName());
        assertEquals(0.95, t.getCpu(),0.1);
    }

    @Test
    public void cpuConsumingThreadsCompare() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_with_deadlocks.log"), new DefaultProgressListener());
        ThreadDumpAnalyzer tda2 = new ThreadDumpAnalyzer(pathOfResource("jstack_11_with_deadlocks_copy_cpu_compare.log"), new DefaultProgressListener());
        
        List<VThread> cpuConsumingThreads = tda.cpuConsumingThreadsCompare(tda2, 5, null);
        assertEquals(5,cpuConsumingThreads.size());
        
        VThread t = cpuConsumingThreads.get(0);
        assertEquals("C2 CompilerThread0", t.getName());
        assertEquals(10000, t.getCpu(),0.1);

        t = cpuConsumingThreads.get(1);
        assertEquals("Thread-0", t.getName());
        assertEquals(5000, t.getCpu(),0.1);

        t = cpuConsumingThreads.get(2);
        assertEquals("main", t.getName());
        assertEquals(1000, t.getCpu(),0.1);

        t = cpuConsumingThreads.get(3);
        assertEquals("Finalizer", t.getName());
        assertEquals(23, t.getCpu(),0.1);
    }

    @Test
    public void cpuConsumingThreadsCompareWithFilter() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_with_deadlocks.log"), new DefaultProgressListener());
        ThreadDumpAnalyzer tda2 = new ThreadDumpAnalyzer(pathOfResource("jstack_11_with_deadlocks_copy_cpu_compare.log"), new DefaultProgressListener());
        
        List<VThread> cpuConsumingThreads = tda.cpuConsumingThreadsCompare(tda2, 5, ThreadType.JAVA);
        assertEquals(5,cpuConsumingThreads.size());
        
        VThread t = cpuConsumingThreads.get(0);
        assertEquals("Thread-0", t.getName());
        assertEquals(5000, t.getCpu(),0.1);

        t = cpuConsumingThreads.get(1);
        assertEquals("main", t.getName());
        assertEquals(1000, t.getCpu(),0.1);

        t = cpuConsumingThreads.get(2);
        assertEquals("Finalizer", t.getName());
        assertEquals(23, t.getCpu(),0.1);
    }

    @Test
    public void testSearch() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_with_deadlocks.log"), new DefaultProgressListener());
        List<VSearchResult> result = tda.search(SearchQuery.forTerms("ReferenceQueue").build());
        assertEquals(2,result.size());
        assertEquals("Finalizer",result.get(0).getName());
        assertEquals("Common-Cleaner", result.get(1).getName());

        result = tda.search(SearchQuery.forTerms("ReferenceQueue").withAllowedJavaStates(EnumSet.of(JavaThreadState.IN_OBJECT_WAIT_TIMED)).build());
        assertEquals("Common-Cleaner", result.get(0).getName());
    }

    @Test
    public void testSearchRegex() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_with_deadlocks.log"), new DefaultProgressListener());
        List<VSearchResult> result = tda.search(SearchQuery.forTerms("Ref.*Queue").withRegex(true).build());
        assertEquals(2,result.size());
        assertEquals("Finalizer",result.get(0).getName());
        assertEquals("Common-Cleaner", result.get(1).getName());

        assertEquals(0, tda.search(SearchQuery.forTerms("Ref.*queue").withRegex(true).withMatchCase(true).build()).size());
        assertEquals(2, tda.search(SearchQuery.forTerms("Ref.*queue").withRegex(true).withMatchCase(false).build()).size());
    }

    @Test
    public void testSearchMatchCase() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_with_deadlocks.log"), new DefaultProgressListener());
        List<VSearchResult> result = tda.search(SearchQuery.forTerms("Re").withMatchCase(false).build());
        assertEquals(13,result.size());

        result = tda.search(SearchQuery.forTerms("Re").withMatchCase(true).build());
        assertEquals(5,result.size());
    }

    @Test
    public void testSearchClassAndMethod() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_with_deadlocks.log"), new DefaultProgressListener());
        List<VSearchResult> result = tda.search(SearchQuery.forTerms("Reference.waitForReferencePendingList").build());
        assertEquals(1,result.size());
    }

    @Test
    public void testDiagnoseDefaultConfigDeadlock() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_with_deadlocks.log"), new DefaultProgressListener());
        List<Diagnostic> result = tda.diagnose(new ThreadDumpAnalysisConfig());
        assertEquals(1,result.size());
        assertEquals("2 threads are in a deadlock",result.get(0).getMessage());
    }

    @Test
    public void testDiagnoseDefaultConfigLarge() throws Exception {
        ThreadDumpAnalyzer tda = new ThreadDumpAnalyzer(pathOfResource("jstack_11_large_with_blocked.log"), new DefaultProgressListener());
        List<Diagnostic> result = tda.diagnose(new ThreadDumpAnalysisConfig());
        assertEquals(2,result.size());
        assertEquals("27 threads are blocked",result.get(0).getMessage());
        assertEquals("5 threads have a very large stack size (> 200)",result.get(1).getMessage());
    }
    
}
