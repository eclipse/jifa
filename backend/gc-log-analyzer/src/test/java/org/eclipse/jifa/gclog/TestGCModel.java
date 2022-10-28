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

package org.eclipse.jifa.gclog;

import org.eclipse.jifa.common.listener.DefaultProgressListener;
import org.eclipse.jifa.gclog.event.*;
import org.eclipse.jifa.gclog.event.evnetInfo.GCCause;
import org.eclipse.jifa.gclog.event.evnetInfo.GCMemoryItem;
import org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType;
import org.eclipse.jifa.gclog.model.*;
import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.jifa.gclog.model.modeInfo.GCLogStyle;
import org.eclipse.jifa.gclog.model.modeInfo.VmOptions;
import org.eclipse.jifa.gclog.parser.GCLogParser;
import org.eclipse.jifa.gclog.parser.GCLogParserFactory;
import org.eclipse.jifa.gclog.parser.JDK8G1GCLogParser;
import org.eclipse.jifa.gclog.util.Constant;
import org.eclipse.jifa.gclog.vo.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.eclipse.jifa.gclog.TestUtil.stringToBufferedReader;
import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.*;
import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_DOUBLE;

public class TestGCModel {

    public static final double DELTA = 1e-6;
    G1GCModel model;

    @Before
    public void mockModel() {
        model = new G1GCModel();
        model.setHeapRegionSize(1024 * 1024);
        model.setCollectorType(GCCollectorType.G1);
        model.setLogStyle(GCLogStyle.PRE_UNIFIED);
        model.setHeapRegionSize(1024 * 1024);
        model.setStartTime(1.0 * 1000);
        model.setParallelThread(8);
        model.setVmOptions(new VmOptions("-Xmx2g -Xms2g"));
        model.setReferenceTimestamp(1000.0);

        GCEvent[] events = new GCEvent[6];
        for (int i = 0; i < 6; i++) {
            events[i] = model.createAndGetEvent();
        }

        events[0].setEventType(GCEventType.YOUNG_GC);
        events[0].setStartTime(1.0 * 1000);
        events[0].setDuration(0.5 * 1000);
        events[0].setCause("G1 Evacuation Pause");
        events[0].setMemoryItem(new GCMemoryItem(EDEN, 20 * 1024 * 1024, 0, 100 * 1024 * 1024));
        events[0].setMemoryItem(new GCMemoryItem(SURVIVOR, 0, 10 * 1024 * 1024, 100 * 1024 * 1024));
        events[0].setMemoryItem(new GCMemoryItem(HUMONGOUS, 10 * 1024 * 1024, 10 * 1024 * 1024, Constant.UNKNOWN_INT));
        events[0].setMemoryItem(new GCMemoryItem(METASPACE, 15 * 1024 * 1024, 15 * 1024 * 1024, 20 * 1024 * 1024));
        events[0].setMemoryItem(new GCMemoryItem(OLD, 10 * 1024 * 1024, 12 * 1024 * 1024, 100 * 1024 * 1024));
        events[0].setTrue(GCEventBooleanType.TO_SPACE_EXHAUSTED);
//      expected result:  collectionResult.addItem(new GCCollectionResultItem(TOTAL,40*1024,32*1024,300*1024));

        events[1].setEventType(GCEventType.FULL_GC);
        events[1].setStartTime(3.0 * 1000);
        events[1].setDuration(0.4 * 1000);
        events[1].setCause("G1 Evacuation Pause");
        events[1].setMemoryItem(new GCMemoryItem(EDEN, 30 * 1024 * 1024, 0, 100 * 1024 * 1024));
        events[1].setMemoryItem(new GCMemoryItem(SURVIVOR, 0, 0, 100 * 1024 * 1024));
        events[1].setMemoryItem(new GCMemoryItem(HUMONGOUS, 10 * 1024 * 1024, 10 * 1024 * 1024, Constant.UNKNOWN_INT));
        events[1].setMemoryItem(new GCMemoryItem(OLD, 10 * 1024 * 1024, 30 * 1024 * 1024, 100 * 1024 * 1024));
//      expected result:  events[1].setMemoryItem(new GCCollectionResultItem(HEAP,50,40,300));

        events[2].setEventType(FULL_GC);
        events[2].setStartTime(12.0 * 1000);
        events[2].setDuration(1.0 * 1000);
        events[2].setCause("Metadata GC Threshold");
        events[2].setMemoryItem(new GCMemoryItem(EDEN, 24 * 1024 * 1024, 0, 100 * 1024 * 1024));
        events[2].setMemoryItem(new GCMemoryItem(SURVIVOR, 8 * 1024 * 1024, 0, 100 * 1024 * 1024));
        events[2].setMemoryItem(new GCMemoryItem(HUMONGOUS, 20 * 1024 * 1024, 20 * 1024 * 1024, Constant.UNKNOWN_INT));
        events[2].setMemoryItem(new GCMemoryItem(OLD, 10 * 1024 * 1024, 10 * 1024 * 1024, 100 * 1024 * 1024));
        events[2].setMemoryItem(new GCMemoryItem(HEAP, 62 * 1024 * 1024, 30 * 1024 * 1024, 300 * 1024 * 1024));

        events[3].setEventType(G1_CONCURRENT_CYCLE);
        events[3].setStartTime(16.0 * 1000);
        events[3].setDuration(1.2 * 1000);
        GCEventType[] concurrentCyclePhases = {G1_REMARK, G1_PAUSE_CLEANUP, G1_CONCURRENT_CLEAR_CLAIMED_MARKS};
        double[] begins = {16.0, 16.2, 16.6};
        for (int i = 0; i < concurrentCyclePhases.length; i++) {
            GCEvent phase = new GCEvent();
            phase.setGcid(4);
            model.addPhase(events[3], phase);
            phase.setEventType(concurrentCyclePhases[i]);
            phase.setDuration(0.2 * (i + 1) * 1000);
            phase.setStartTime(begins[i] * 1000);
        }
        events[3].getPhases().get(0).setMemoryItem(new GCMemoryItem(HEAP, 70 * 1024 * 1024, 70 * 1024 * 1024, 300 * 1024 * 1024));
        events[3].getPhases().get(1).setMemoryItem(new GCMemoryItem(HEAP, 70 * 1024 * 1024, 70 * 1024 * 1024, 300 * 1024 * 1024));

        events[4].setEventType(G1_CONCURRENT_CYCLE);
        events[4].setStartTime(24 * 1000);
        events[4].setDuration(0.6 * 1000);
        concurrentCyclePhases = new GCEventType[]{G1_REMARK, G1_PAUSE_CLEANUP, G1_CONCURRENT_CLEAR_CLAIMED_MARKS};
        begins = new double[]{24.0, 24.1, 24.3};
        for (int i = 0; i < concurrentCyclePhases.length; i++) {
            GCEvent phase = new GCEvent();
            phase.setGcid(5);
            model.addPhase(events[4], phase);
            phase.setEventType(concurrentCyclePhases[i]);
            phase.setDuration(0.1 * (i + 1) * 1000);
            phase.setStartTime(begins[i] * 1000);
        }
        events[4].getPhases().get(0).setMemoryItem(new GCMemoryItem(HEAP, 70 * 1024 * 1024, 70 * 1024 * 1024, 300 * 1024 * 1024));
        events[4].getPhases().get(1).setMemoryItem(new GCMemoryItem(HEAP, 70 * 1024 * 1024, 70 * 1024 * 1024, 300 * 1024 * 1024));

        events[5].setEventType(YOUNG_GC);
        events[5].setStartTime(32.0 * 1000);
        events[5].setDuration(0.3 * 1000);
        events[5].setCause("G1 Evacuation Pause");
        events[5].setMemoryItem(new GCMemoryItem(EDEN, 16 * 1024 * 1024, 0, 100 * 1024 * 1024));
        events[5].setMemoryItem(new GCMemoryItem(SURVIVOR, 0, 4 * 1024 * 1024, 100 * 1024 * 1024));
        events[5].setMemoryItem(new GCMemoryItem(HUMONGOUS, 50 * 1024 * 1024, 50 * 1024 * 1024, Constant.UNKNOWN_INT));
        events[5].setMemoryItem(new GCMemoryItem(METASPACE, 15 * 1024 * 1024, 15 * 1024 * 1024, 40 * 1024 * 1024));
//      expected result:  events[5].setMemoryItem(new GCCollectionResultItem(OLD,10,12,100));
        events[5].setMemoryItem(new GCMemoryItem(HEAP, 76 * 1024 * 1024, 66 * 1024 * 1024, 300 * 1024 * 1024));

        Safepoint safepoint = new Safepoint();
        safepoint.setStartTime(31.9 * 1000);
        safepoint.setTimeToEnter(0.6 * 1000);
        safepoint.setDuration(0.3 * 1000);
        model.addSafepoint(safepoint);
    }

    @Test
    public void testModelMisc() {
        model.calculateDerivedInfo(new DefaultProgressListener());

        // derived model info
        Assert.assertEquals(model.getEndTime(), 32.3 * 1000, DELTA);
        Assert.assertEquals(model.getGcCollectionEvents().size(), 8);

        // single event info
        Assert.assertEquals(model.getGcEvents().get(0).getAllocation(), 40 * 1024 * 1024);
        Assert.assertEquals(model.getGcEvents().get(0).getPromotion(), 2 * 1024 * 1024);
        Assert.assertEquals(model.getGcEvents().get(0).getReclamation(), 8 * 1024 * 1024);
        Assert.assertEquals(model.getGcEvents().get(3).getPause(), 0.6 * 1000, DELTA);
        Assert.assertEquals(model.getGcEvents().get(4).getPause(), 0.3 * 1000, DELTA);
        Assert.assertEquals(model.getGcEvents().get(0).getMemoryItem(HEAP),
                new GCMemoryItem(HEAP, 40 * 1024 * 1024, 32 * 1024 * 1024, 300 * 1024 * 1024));
        Assert.assertEquals(model.getGcEvents().get(5).getMemoryItem(OLD),
                new GCMemoryItem(OLD, 10 * 1024 * 1024, 12 * 1024 * 1024, 100 * 1024 * 1024));
        Assert.assertEquals(model.getGcEvents().get(4).getInterval(), 6.8 * 1000, DELTA);

        // object statistics
        ObjectStatistics objectStatistics = model.getObjectStatistics(new TimeRange(500, 33000));
        Assert.assertEquals(objectStatistics.getObjectCreationSpeed(), (40 + 18 + 22 + 40 + 6) * 1024 * 1024 / 31800.0, DELTA);
        Assert.assertEquals(objectStatistics.getObjectPromotionSpeed(), (2 + 2) * 1024 * 1024 / 31800.0, DELTA);
        Assert.assertEquals(objectStatistics.getObjectPromotionAvg(), 2 * 1024 * 1024);
        Assert.assertEquals(objectStatistics.getObjectPromotionMax(), 2 * 1024 * 1024);

        // pause statistics
        PauseStatistics pauseStatistics = model.getPauseStatistics(new TimeRange(10000, 30000));
        Assert.assertEquals(pauseStatistics.getThroughput(), 1 - (1000 + 100 + 200 + 200 + 400) / 20000.0, DELTA);
        Assert.assertEquals(pauseStatistics.getPauseAvg(), (1000 + 100 + 200 + 200 + 400) / 5.0, DELTA);
        Assert.assertEquals(pauseStatistics.getPauseMedian(), 200, DELTA);
        Assert.assertEquals(pauseStatistics.getPauseP99(), 0.96 * 1000 + 0.04 * 400, DELTA);
        Assert.assertEquals(pauseStatistics.getPauseP999(), 0.996 * 1000 + 0.004 * 400, DELTA);
        Assert.assertEquals(pauseStatistics.getPauseMax(), 1000.0, DELTA);

        // pause Distribution
        Map<String, int[]> pauseDistribution = model.getPauseDistribution(new TimeRange(10000, 30000), new int[]{0, 300});
        Assert.assertArrayEquals(pauseDistribution.get(FULL_GC.getName()), new int[]{0, 1});
        Assert.assertArrayEquals(pauseDistribution.get(G1_REMARK.getName()), new int[]{2, 0});
        Assert.assertArrayEquals(pauseDistribution.get(G1_PAUSE_CLEANUP.getName()), new int[]{1, 1});

        // phase statistics
        List<PhaseStatistics.ParentStatisticsInfo> parents = model.getPhaseStatistics(new TimeRange(0, 999999999)).getParents();
        Assert.assertEquals(parents.size(), 3);
        Assert.assertEquals(parents.get(0).getCauses().size(), 1);
        Assert.assertEquals(parents.get(2).getCauses().size(), 2);
        Assert.assertEquals(parents.get(0).getCauses().get(0), new PhaseStatistics.PhaseStatisticItem(
                "G1 Evacuation Pause", 2, 30500, 30500, 400, 500, 800));
        Assert.assertEquals(parents.get(1).getSelf(), new PhaseStatistics.PhaseStatisticItem(
                G1_CONCURRENT_CYCLE.getName(), 2, 6800, 6800, 900, 1200, 1800));
        Assert.assertEquals(parents.get(1).getPhases().size(), 3);
        Assert.assertEquals(parents.get(1).getPhases().get(1), new PhaseStatistics.PhaseStatisticItem(
                G1_REMARK.getName(), 2, 7800, 7800, 150, 200, 300));

        Map<String, List<Object[]>> graphData = model.getTimeGraphData(new String[]{"youngCapacity", "heapUsed", "reclamation", "promotion", G1_REMARK.getName()});
        Assert.assertEquals(graphData.size(), 5);

        List<Object[]> youngCapacity = graphData.get("youngCapacity");
        Assert.assertEquals(youngCapacity.size(), 4);
        Assert.assertArrayEquals(youngCapacity.get(3), new Object[]{32300L, 200L * 1024 * 1024,});

        List<Object[]> heapUse = graphData.get("heapUsed");
        Assert.assertEquals(heapUse.size(), 16);
        Assert.assertArrayEquals(heapUse.get(14), new Object[]{32000L, 76L * 1024 * 1024,});
        Assert.assertArrayEquals(heapUse.get(15), new Object[]{32300L, 66L * 1024 * 1024,});

        List<Object[]> reclamation = graphData.get("reclamation");
        Assert.assertEquals(reclamation.size(), 8);
        Assert.assertArrayEquals(reclamation.get(7), new Object[]{32000L, 10L * 1024 * 1024,});

        List<Object[]> promotion = graphData.get("promotion");
        Assert.assertEquals(promotion.size(), 2);
        Assert.assertArrayEquals(promotion.get(1), new Object[]{32000L, 2L * 1024 * 1024,});

        List<Object[]> remark = graphData.get(G1_REMARK.getName());
        Assert.assertEquals(remark.size(), 2);
        Assert.assertArrayEquals(remark.get(1), new Object[]{24000L, 100.0,});
    }

    @Test
    public void testUseCPUTimeAsPause() throws Exception {
        String log = "2022-05-23T11:29:31.538+0800: 224076.254: [GC pause (G1 Evacuation Pause) (young), 0.6017393 secs]\n" +
                "   [Parallel Time: 21.6 ms, GC Workers: 13]\n" +
                "      [GC Worker Start (ms): Min: 224076603.9, Avg: 224076604.1, Max: 224076604.3, Diff: 0.4]\n" +
                "      [Ext Root Scanning (ms): Min: 1.7, Avg: 2.2, Max: 3.5, Diff: 1.8, Sum: 29.2]\n" +
                "      [Update RS (ms): Min: 2.6, Avg: 3.9, Max: 4.2, Diff: 1.6, Sum: 50.7]\n" +
                "         [Processed Buffers: Min: 21, Avg: 36.2, Max: 50, Diff: 29, Sum: 470]\n" +
                "      [Scan RS (ms): Min: 0.1, Avg: 0.3, Max: 0.3, Diff: 0.3, Sum: 3.3]\n" +
                "      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]\n" +
                "      [Object Copy (ms): Min: 14.0, Avg: 14.3, Max: 14.4, Diff: 0.4, Sum: 186.2]\n" +
                "      [Termination (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]\n" +
                "         [Termination Attempts: Min: 2, Avg: 5.5, Max: 11, Diff: 9, Sum: 72]\n" +
                "      [GC Worker Other (ms): Min: 0.1, Avg: 0.3, Max: 0.5, Diff: 0.4, Sum: 3.6]\n" +
                "      [GC Worker Total (ms): Min: 20.7, Avg: 21.0, Max: 21.3, Diff: 0.7, Sum: 273.2]\n" +
                "      [GC Worker End (ms): Min: 224076625.0, Avg: 224076625.2, Max: 224076625.4, Diff: 0.4]\n" +
                "   [Code Root Fixup: 0.1 ms]\n" +
                "   [Code Root Purge: 0.0 ms]\n" +
                "   [Clear CT: 1.1 ms]\n" +
                "   [Other: 578.9 ms]\n" +
                "      [Choose CSet: 0.0 ms]\n" +
                "      [Ref Proc: 2.6 ms]\n" +
                "      [Ref Enq: 0.0 ms]\n" +
                "      [Redirty Cards: 0.6 ms]\n" +
                "      [Humongous Register: 0.1 ms]\n" +
                "      [Humongous Reclaim: 0.1 ms]\n" +
                "      [Free CSet: 2.1 ms]\n" +
                "   [Eden: 2924.0M(2924.0M)->0.0B(2924.0M) Survivors: 148.0M->148.0M Heap: 3924.7M(5120.0M)->1006.9M(5120.0M)]\n" +
                "Heap after GC invocations=1602 (full 0):\n" +
                " garbage-first heap   total 5242880K, used 1031020K [0x0000000680000000, 0x0000000680205000, 0x00000007c0000000)\n" +
                "  region size 2048K, 74 young (151552K), 74 survivors (151552K)\n" +
                " Metaspace       used 120752K, capacity 128436K, committed 129536K, reserved 1161216K\n" +
                "  class space    used 14475K, capacity 16875K, committed 17152K, reserved 1048576K\n" +
                "}\n" +
                " [Times: user=0.29 sys=0.00, real=0.71 secs] ";
        JDK8G1GCLogParser parser = (JDK8G1GCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        G1GCModel model = (G1GCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().get(0).getPause(), 710, DELTA);
    }

    @Test
    public void testCauseInterval() throws Exception {
        String log = "0.003: [GC (Allocation Failure) 0.003: [ParNew: 1826919K->78900K(1922432K), 0.0572643 secs] 3445819K->1697799K(4019584K), 0.0575802 secs] [Times: user=0.21 sys=0.00, real=0.05 secs]\n" +
                "12.765: [Full GC (Last ditch collection) 12.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]\n" +
                "80.765: [Full GC (System.gc()) 80.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]\n" +
                "95.765: [Full GC (Allocation Failure) 95.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]\n" +
                "103.765: [Full GC (Metadata GC Threshold) 103.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]\n" +
                "120.765: [Full GC (Allocation Failure) 120.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]\n" +
                "155.765: [Full GC (Allocation Failure) 155.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]";
        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        GCModel model = parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assert.assertEquals(model.getGcEvents().get(3).getCauseInterval(), UNKNOWN_DOUBLE, DELTA);
        Assert.assertEquals(model.getGcEvents().get(5).getCauseInterval(), 24884.9386, DELTA);
        Assert.assertEquals(model.getGcEvents().get(6).getCauseInterval(), 34884.9386, DELTA);

        List<PhaseStatistics.ParentStatisticsInfo> parents = model.getPhaseStatistics(new TimeRange(0, 99999999)).getParents();
        for (PhaseStatistics.ParentStatisticsInfo parent : parents) {
            if (parent.getSelf().getName().equals(FULL_GC.getName())) {
                for (PhaseStatistics.PhaseStatisticItem cause : parent.getCauses()) {
                    if (cause.getName().equals(GCCause.ALLOCATION_FAILURE.getName())) {
                        Assert.assertEquals(cause.getCount(), 3);
                        Assert.assertEquals(cause.getIntervalMin(), 24884.9386, DELTA);
                        Assert.assertEquals(cause.getIntervalAvg(), (24884.9386 + 34884.9386) / 2, DELTA);
                        return;
                    }
                }
            }
        }
        Assert.fail("should find full gc with Allocation Failure");
    }
}
