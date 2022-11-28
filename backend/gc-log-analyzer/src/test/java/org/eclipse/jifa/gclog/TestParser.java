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
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.event.ThreadEvent;
import org.eclipse.jifa.gclog.event.Safepoint;
import org.eclipse.jifa.gclog.model.*;
import org.eclipse.jifa.gclog.model.modeInfo.GCLogMetadata;
import org.eclipse.jifa.gclog.parser.*;
import org.eclipse.jifa.gclog.event.evnetInfo.GCMemoryItem;
import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.jifa.gclog.parser.GCLogParsingMetadata;
import org.eclipse.jifa.gclog.model.modeInfo.GCLogStyle;
import org.eclipse.jifa.gclog.vo.PauseStatistics;
import org.eclipse.jifa.gclog.vo.TimeRange;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType.YOUNG_GC_BECOME_FULL_GC;
import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_DOUBLE;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.*;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType.TO_SPACE_EXHAUSTED;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.*;
import static org.eclipse.jifa.gclog.TestUtil.stringToBufferedReader;
import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_INT;

public class TestParser {

    public static final double DELTA = 1e-6;

    @Test
    public void testJDK11G1Parser() throws Exception {
        String log =
                "[0.015s][info][gc,heap] Heap region size: 1M\n" +
                        "[0.017s][info][gc     ] Using G1\n" +
                        "[0.017s][info][gc,heap,coops] Heap address: 0x00000007fc000000, size: 64 MB, Compressed Oops mode: Zero based, Oop shift amount: 3\n" +
                        "[0.050s][info   ][gc           ] Periodic GC enabled with interval 100 ms" +
                        "[1.000s][info][safepoint     ] Application time: 0.0816788 seconds\n" +
                        "[1.000s][info][safepoint     ] Entering safepoint region: G1CollectForAllocation\n" +
                        "[1.000s][info][gc,start     ] GC(0) Pause Young (Normal) (Metadata GC Threshold)\n" +
                        "[1.000s][info][gc,task      ] GC(0) Using 8 workers of 8 for evacuation\n" +
                        "[1.010s][info][gc           ] GC(0) To-space exhausted\n" +
                        "[1.010s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms\n" +
                        "[1.010s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 9.5ms\n" +
                        "[1.010s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 0.6ms\n" +
                        "[1.010s][info][gc,phases    ] GC(0)   Other: 0.5ms\n" +
                        "[1.010s][info][gc,heap      ] GC(0) Eden regions: 19->0(33)\n" +
                        "[1.010s][info][gc,heap      ] GC(0) Survivor regions: 0->3(3)\n" +
                        "[1.010s][info][gc,heap      ] GC(0) Old regions: 0->2\n" +
                        "[1.010s][info][gc,heap      ] GC(0) Humongous regions: 4->3\n" +
                        "[1.010s][info][gc,metaspace ] GC(0) Metaspace: 20679K->20679K(45056K)\n" +
                        "[1.010s][info][gc           ] GC(0) Pause Young (Concurrent Start) (Metadata GC Threshold) 19M->4M(64M) 10.709ms\n" +
                        "[1.010s][info][gc,cpu       ] GC(0) User=0.02s Sys=0.01s Real=0.01s\n" +
                        "[1.010s][info][safepoint     ] Leaving safepoint region\n" +
                        "[1.010s][info][safepoint     ] Total time for which application threads were stopped: 0.0101229 seconds, Stopping threads took: 0.0000077 seconds\n" +
                        "[3.000s][info][gc           ] GC(1) Concurrent Cycle\n" +
                        "[3.000s][info][gc,marking   ] GC(1) Concurrent Clear Claimed Marks\n" +
                        "[3.000s][info][gc,marking   ] GC(1) Concurrent Clear Claimed Marks 0.057ms\n" +
                        "[3.000s][info][gc,marking   ] GC(1) Concurrent Scan Root Regions\n" +
                        "[3.002s][info][gc,marking   ] GC(1) Concurrent Scan Root Regions 2.709ms\n" +
                        "[3.002s][info][gc,marking   ] GC(1) Concurrent Mark (3.002s)\n" +
                        "[3.002s][info][gc,marking   ] GC(1) Concurrent Mark From Roots\n" +
                        "[3.002s][info][gc,task      ] GC(1) Using 2 workers of 2 for marking\n" +
                        "[3.005s][info][gc,marking   ] GC(1) Concurrent Mark From Roots 3.109ms\n" +
                        "[3.005s][info][gc,marking   ] GC(1) Concurrent Preclean\n" +
                        "[3.005s][info][gc,marking   ] GC(1) Concurrent Preclean 0.040ms\n" +
                        "[3.005s][info][gc,marking   ] GC(1) Concurrent Mark (2.391s, 2.394s) 3.251ms\n" +
                        "[3.005s][info][gc,start     ] GC(1) Pause Remark\n" +
                        "[3.005s][info][gc,stringtable] GC(1) Cleaned string and symbol table, strings: 9850 processed, 0 removed, symbols: 69396 processed, 29 removed\n" +
                        "[3.008s][info][gc            ] GC(1) Pause Remark 5M->5M(64M) 2.381ms\n" +
                        "[3.008s][info][gc,cpu        ] GC(1) User=0.01s Sys=0.00s Real=0.01s\n" +
                        "[3.008s][info][gc,marking    ] GC(1) Concurrent Rebuild Remembered Sets\n" +
                        "[3.010s][info][gc,marking    ] GC(1) Concurrent Rebuild Remembered Sets 2.151ms\n" +
                        "[3.010s][info][gc,start      ] GC(1) Pause Cleanup\n" +
                        "[3.010s][info][gc            ] GC(1) Pause Cleanup 6M->6M(64M) 0.094ms\n" +
                        "[3.010s][info][gc,cpu        ] GC(1) User=0.00s Sys=0.00s Real=0.00s\n" +
                        "[3.010s][info][gc,marking    ] GC(1) Concurrent Cleanup for Next Mark\n" +
                        "[3.012s][info][gc,marking    ] GC(1) Concurrent Cleanup for Next Mark 2.860ms\n" +
                        "[3.012s][info][gc            ] GC(1) Concurrent Cycle 14.256ms\n" +
                        "[7.055s][info   ][gc,task       ] GC(2) Using 8 workers of 8 for full compaction\n" +
                        "[7.055s][info   ][gc,start      ] GC(2) Pause Full (G1 Evacuation Pause)\n" +
                        "[7.056s][info   ][gc,phases,start] GC(2) Phase 1: Mark live objects\n" +
                        "[7.058s][info   ][gc,stringtable ] GC(2) Cleaned string and symbol table, strings: 1393 processed, 0 removed, symbols: 17391 processed, 0 removed\n" +
                        "[7.058s][info   ][gc,phases      ] GC(2) Phase 1: Mark live objects 2.650ms\n" +
                        "[7.058s][info   ][gc,phases,start] GC(2) Phase 2: Prepare for compaction\n" +
                        "[7.061s][info   ][gc,phases      ] GC(2) Phase 2: Prepare for compaction 2.890ms\n" +
                        "[7.061s][info   ][gc,phases,start] GC(2) Phase 3: Adjust pointers\n" +
                        "[7.065s][info   ][gc,phases      ] GC(2) Phase 3: Adjust pointers 3.890ms\n" +
                        "[7.065s][info   ][gc,phases,start] GC(2) Phase 4: Compact heap\n" +
                        "[7.123s][info   ][gc,phases      ] GC(2) Phase 4: Compact heap 57.656ms\n" +
                        "[7.123s][info   ][gc,heap        ] GC(2) Eden regions: 0->0(680)\n" +
                        "[7.123s][info   ][gc,heap        ] GC(2) Survivor regions: 0->0(85)\n" +
                        "[7.123s][info   ][gc,heap        ] GC(2) Old regions: 1700->1089\n" +
                        "[7.123s][info   ][gc,heap        ] GC(2) Humongous regions: 0->0\n" +
                        "[7.123s][info   ][gc,metaspace   ] GC(2) Metaspace: 3604K->3604K(262144K)\n" +
                        "[7.123s][info   ][gc             ] GC(2) Pause Full (G1 Evacuation Pause) 1700M->1078M(1700M) 67.806ms\n" +
                        "[7.123s][info   ][gc,cpu         ] GC(2) User=0.33s Sys=0.00s Real=0.07s";
        UnifiedG1GCLogParser parser = (UnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));
        G1GCModel model = (G1GCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        // assert parsing success
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 3);

        // assert model info
        Assert.assertEquals(model.getStartTime(), 0.0 * 1000, DELTA);
        Assert.assertEquals(model.getEndTime(), 7.123 * 1000, DELTA);
        Assert.assertEquals(model.getCollectorType(), GCCollectorType.G1);

        Assert.assertEquals(model.getHeapRegionSize(), 1024 * 1024);
        Assert.assertNull(model.getVmOptions());
        Assert.assertEquals(model.getParallelThread(), 8);
        Assert.assertEquals(model.getConcurrentThread(), 2);

        // assert events correct
        Assert.assertEquals(model.getSafepoints().size(), 1);

        Safepoint safepoint = model.getSafepoints().get(0);
        Assert.assertEquals(safepoint.getStartTime(), 1010 - 10.1229, DELTA);
        Assert.assertEquals(safepoint.getDuration(), 10.1229, DELTA);
        Assert.assertEquals(safepoint.getTimeToEnter(), 0.0077, DELTA);

        List<GCEvent> event = model.getGcEvents();
        GCEvent youngGC = event.get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertTrue(youngGC.isTrue(TO_SPACE_EXHAUSTED));
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getStartTime(), 1.0 * 1000, DELTA);
        Assert.assertEquals(youngGC.getPause(), 10.709, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 10.709, DELTA);
        Assert.assertEquals(youngGC.getCause(), METADATA_GENERATION_THRESHOLD);
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 0.01 * 1000, DELTA);
        Assert.assertEquals(youngGC.getPhases().size(), 4);
        Assert.assertEquals(youngGC.getPhases().get(1).getEventType(), GCEventType.G1_COLLECT_EVACUATION);
        Assert.assertEquals(youngGC.getPhases().get(1).getDuration(), 9.5, DELTA);
        Assert.assertEquals(youngGC.getMemoryItem(SURVIVOR)
                , new GCMemoryItem(SURVIVOR, 0 * 1024, 3 * 1024 * 1024, 3 * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(METASPACE)
                , new GCMemoryItem(METASPACE, 20679 * 1024, 20679 * 1024, 45056 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(HEAP)
                , new GCMemoryItem(HEAP, 19 * 1024 * 1024, 4 * 1024 * 1024, 64 * 1024 * 1024));
        Assert.assertTrue(youngGC.toString().contains("To-space Exhausted"));

        GCEvent concurrentMark = event.get(1);
        Assert.assertEquals(concurrentMark.getGcid(), 1);
        Assert.assertEquals(concurrentMark.getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assert.assertEquals(concurrentMark.getDuration(), 14.256, DELTA);
        Assert.assertEquals(concurrentMark.getPause(), 2.381 + 0.094, DELTA);
        Assert.assertEquals(concurrentMark.getPhases().get(0).getEventType(), GCEventType.G1_CONCURRENT_CLEAR_CLAIMED_MARKS);
        Assert.assertEquals(concurrentMark.getPhases().get(0).getDuration(), 0.057, DELTA);

        GCEvent fullGC = event.get(2);
        Assert.assertEquals(fullGC.getGcid(), 2);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getStartTime(), 7.055 * 1000, DELTA);
        Assert.assertEquals(fullGC.getPause(), 67.806, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 67.806, DELTA);
        Assert.assertEquals(fullGC.getCause(), G1_EVACUATION_PAUSE);
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        Assert.assertEquals(fullGC.getPhases().get(3).getEventType(), GCEventType.G1_COMPACT_HEAP);
        Assert.assertEquals(fullGC.getPhases().get(3).getDuration(), 57.656, DELTA);

        GCLogMetadata metadata = model.getGcModelMetadata();
        Assert.assertTrue(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS.getName()));
        Assert.assertFalse(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_UNDO_CYCLE.getName()));
    }

    @Test
    public void testJDK11G1ParserDetectHeapRegionSize() throws Exception {
        String log = "[3.865s][info][gc,start      ] GC(14) Pause Young (Normal) (G1 Evacuation Pause)\n" +
                "[3.865s][info][gc,task       ] GC(14) Using 2 workers of 2 for evacuation\n" +
                "[3.982s][info][gc,phases     ] GC(14)   Pre Evacuate Collection Set: 0.0ms\n" +
                "[3.982s][info][gc,phases     ] GC(14)   Evacuate Collection Set: 116.2ms\n" +
                "[3.982s][info][gc,phases     ] GC(14)   Post Evacuate Collection Set: 0.3ms\n" +
                "[3.982s][info][gc,phases     ] GC(14)   Other: 0.2ms\n" +
                "[3.982s][info][gc,heap       ] GC(14) Eden regions: 5->0(5)\n" +
                "[3.982s][info][gc,heap       ] GC(14) Survivor regions: 1->1(1)\n" +
                "[3.982s][info][gc,heap       ] GC(14) Old regions: 32->37\n" +
                "[3.982s][info][gc,heap       ] GC(14) Humongous regions: 2->2\n" +
                "[3.982s][info][gc,metaspace  ] GC(14) Metaspace: 21709K->21707K(1069056K)\n" +
                "[3.982s][info][gc            ] GC(14) Pause Young (Normal) (G1 Evacuation Pause) 637M->630M(2048M) 116.771ms";
        UnifiedG1GCLogParser parser = new UnifiedG1GCLogParser();
        parser.setMetadata(new GCLogParsingMetadata(GCCollectorType.G1, GCLogStyle.UNIFIED));
        G1GCModel model = (G1GCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        // should infer region size 16m
        Assert.assertEquals(model.getHeapRegionSize(), 16 * 1024 * 1024);
        Assert.assertEquals(model.getGcEvents().get(0).getMemoryItem(OLD)
                , new GCMemoryItem(OLD, 32 * 16 * 1024 * 1024, 37 * 16 * 1024 * 1024, 1952L * 1024 * 1024));
        Assert.assertEquals(model.getGcEvents().get(0).getMemoryItem(METASPACE)
                , new GCMemoryItem(METASPACE, 21709 * 1024, 21707 * 1024, 1069056 * 1024));
    }

    @Test
    public void testJDK11ParseDecoration() throws Exception {
        String log = "[2021-05-06T11:25:16.508+0800][info][gc           ] GC(0) Pause Young (Concurrent Start) (Metadata GC Threshold)\n" +
                "[2021-05-06T11:25:16.510+0800][info][gc           ] GC(1) Pause Young (Concurrent Start) (Metadata GC Threshold)\n";
        UnifiedG1GCLogParser parser = new UnifiedG1GCLogParser();
        parser.setMetadata(new GCLogParsingMetadata(GCCollectorType.G1, GCLogStyle.UNIFIED));
        GCModel model = parser.parse(stringToBufferedReader(log));
        Assert.assertEquals(model.getReferenceTimestamp(), 1620271516508d, DELTA);
        Assert.assertEquals(model.getGcEvents().get(1).getStartTime(), 2, DELTA);

        log = "[1000000000800ms][info][gc           ] GC(0) Pause Young (Concurrent Start) (Metadata GC Threshold)\n" +
                "[1000000000802ms][info][gc           ] GC(1) Pause Young (Concurrent Start) (Metadata GC Threshold)\n";
        parser = new UnifiedG1GCLogParser();
        parser.setMetadata(new GCLogParsingMetadata(GCCollectorType.G1, GCLogStyle.UNIFIED));
        model = parser.parse(stringToBufferedReader(log));
        Assert.assertEquals(model.getReferenceTimestamp(), 1000000000800D, DELTA);
        Assert.assertEquals(model.getGcEvents().get(1).getStartTime(), 2, DELTA);
    }

    @Test
    public void testJDK11ZGCParser() throws Exception {
        String log =
                "[7.000s] GC(374) Garbage Collection (Proactive)\n" +
                        "[7.006s] GC(374) Pause Mark Start 4.459ms\n" +
                        "[7.312s] GC(374) Concurrent Mark 306.720ms\n" +
                        "[7.312s] GC(374) Pause Mark End 0.606ms\n" +
                        "[7.313s] GC(374) Concurrent Process Non-Strong References 1.290ms\n" +
                        "[7.314s] GC(374) Concurrent Reset Relocation Set 0.550ms\n" +
                        "[7.314s] GC(374) Concurrent Destroy Detached Pages 0.001ms\n" +
                        "[7.316s] GC(374) Concurrent Select Relocation Set 2.418ms\n" +
                        "[7.321s] GC(374) Concurrent Prepare Relocation Set 5.719ms\n" +
                        "[7.324s] GC(374) Pause Relocate Start 3.791ms\n" +
                        "[7.356s] GC(374) Concurrent Relocate 32.974ms\n" +
                        "[7.356s] GC(374) Load: 1.68/1.99/2.04\n" +
                        "[7.356s] GC(374) MMU: 2ms/0.0%, 5ms/0.0%, 10ms/0.0%, 20ms/0.0%, 50ms/0.0%, 100ms/0.0%\n" +
                        "[7.356s] GC(374) Mark: 8 stripe(s), 2 proactive flush(es), 1 terminate flush(es), 0 completion(s), 0 continuation(s)\n" +
                        "[7.356s] GC(374) Relocation: Successful, 359M relocated\n" +
                        "[7.356s] GC(374) NMethods: 21844 registered, 609 unregistered\n" +
                        "[7.356s] GC(374) Metaspace: 125M used, 127M capacity, 128M committed, 130M reserved\n" +
                        "[7.356s] GC(374) Soft: 18634 encountered, 0 discovered, 0 enqueued\n" +
                        "[7.356s] GC(374) Weak: 56186 encountered, 18454 discovered, 3112 enqueued\n" +
                        "[7.356s] GC(374) Final: 64 encountered, 16 discovered, 7 enqueued\n" +
                        "[7.356s] GC(374) Phantom: 1882 encountered, 1585 discovered, 183 enqueued\n" +
                        "[7.356s] GC(374)                Mark Start          Mark End        Relocate Start      Relocate End           High               Low\n" +
                        "[7.356s] GC(374)  Capacity:    40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)\n" +
                        "[7.356s] GC(374)   Reserve:       96M (0%)           96M (0%)           96M (0%)           96M (0%)           96M (0%)           96M (0%)\n" +
                        "[7.356s] GC(374)      Free:    35250M (86%)       35210M (86%)       35964M (88%)       39410M (96%)       39410M (96%)       35210M (86%)\n" +
                        "[7.356s] GC(374)      Used:     5614M (14%)        5654M (14%)        4900M (12%)        1454M (4%)         5654M (14%)        1454M (4%)\n" +
                        "[7.356s] GC(374)      Live:         -              1173M (3%)         1173M (3%)         1173M (3%)             -                  -\n" +
                        "[7.356s] GC(374) Allocated:         -                40M (0%)           40M (0%)          202M (0%)             -                  -\n" +
                        "[7.356s] GC(374)   Garbage:         -              4440M (11%)        3686M (9%)          240M (1%)             -                  -\n" +
                        "[7.356s] GC(374) Reclaimed:         -                  -               754M (2%)         4200M (10%)            -                  -\n" +
                        "[7.356s] GC(374) Garbage Collection (Proactive) 5614M(14%)->1454M(4%)\n" +
                        "[7.555s] === Garbage Collection Statistics =======================================================================================================================\n" +
                        "[7.555s]                                                              Last 10s              Last 10m              Last 10h                Total\n" +
                        "[7.555s]                                                              Avg / Max             Avg / Max             Avg / Max             Avg / Max\n" +
                        "[7.555s]   Collector: Garbage Collection Cycle                    362.677 / 362.677     365.056 / 529.211     315.229 / 868.961     315.229 / 868.961     ms\n" +
                        "[7.555s]  Contention: Mark Segment Reset Contention                     0 / 0                 1 / 106               0 / 238               0 / 238         ops/s\n" +
                        "[7.555s]  Contention: Mark SeqNum Reset Contention                      0 / 0                 0 / 1                 0 / 1                 0 / 1           ops/s\n" +
                        "[7.555s]  Contention: Relocation Contention                             1 / 10                0 / 52                0 / 87                0 / 87          ops/s\n" +
                        "[7.555s]    Critical: Allocation Stall                              0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[7.555s]    Critical: Allocation Stall                                  0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[7.555s]    Critical: GC Locker Stall                               0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[7.555s]    Critical: GC Locker Stall                                   0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[7.555s]      Memory: Allocation Rate                                  85 / 210             104 / 826              54 / 2628             54 / 2628        MB/s\n" +
                        "[7.555s]      Memory: Heap Used After Mark                           5654 / 5654           5727 / 6416           5588 / 14558          5588 / 14558       MB\n" +
                        "[7.555s]      Memory: Heap Used After Relocation                     1454 / 1454           1421 / 1814           1224 / 2202           1224 / 2202        MB\n" +
                        "[7.555s]      Memory: Heap Used Before Mark                          5614 / 5614           5608 / 6206           5503 / 14268          5503 / 14268       MB\n" +
                        "[7.555s]      Memory: Heap Used Before Relocation                    4900 / 4900           4755 / 5516           4665 / 11700          4665 / 11700       MB\n" +
                        "[7.555s]      Memory: Out Of Memory                                     0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[7.555s]      Memory: Page Cache Flush                                  0 / 0                 0 / 0                 0 / 0                 0 / 0           MB/s\n" +
                        "[7.555s]      Memory: Page Cache Hit L1                                49 / 105              53 / 439              27 / 1353             27 / 1353        ops/s\n" +
                        "[7.555s]      Memory: Page Cache Hit L2                                 0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[7.555s]      Memory: Page Cache Miss                                   0 / 0                 0 / 0                 0 / 551               0 / 551         ops/s\n" +
                        "[7.555s]      Memory: Undo Object Allocation Failed                     0 / 0                 0 / 0                 0 / 8                 0 / 8           ops/s\n" +
                        "[7.555s]      Memory: Undo Object Allocation Succeeded                  1 / 10                0 / 52                0 / 87                0 / 87          ops/s\n" +
                        "[7.555s]      Memory: Undo Page Allocation                              0 / 0                 0 / 1                 0 / 16                0 / 16          ops/s\n" +
                        "[7.555s]       Phase: Concurrent Destroy Detached Pages             0.001 / 0.001         0.001 / 0.001         0.001 / 0.012         0.001 / 0.012       ms\n" +
                        "[7.555s]       Phase: Concurrent Mark                             306.720 / 306.720     303.979 / 452.112     255.790 / 601.718     255.790 / 601.718     ms\n" +
                        "[7.555s]       Phase: Concurrent Mark Continue                      0.000 / 0.000         0.000 / 0.000       189.372 / 272.607     189.372 / 272.607     ms\n" +
                        "[7.555s]       Phase: Concurrent Prepare Relocation Set             5.719 / 5.719         6.314 / 14.492        6.150 / 36.507        6.150 / 36.507      ms\n" +
                        "[7.555s]       Phase: Concurrent Process Non-Strong References      1.290 / 1.290         1.212 / 1.657         1.179 / 2.334         1.179 / 2.334       ms\n" +
                        "[7.555s]       Phase: Concurrent Relocate                          32.974 / 32.974       35.964 / 86.278       31.599 / 101.253      31.599 / 101.253     ms\n" +
                        "[7.555s]       Phase: Concurrent Reset Relocation Set               0.550 / 0.550         0.615 / 0.937         0.641 / 5.411         0.641 / 5.411       ms\n" +
                        "[7.555s]       Phase: Concurrent Select Relocation Set              2.418 / 2.418         2.456 / 3.131         2.509 / 4.753         2.509 / 4.753       ms\n" +
                        "[7.555s]       Phase: Pause Mark End                                0.606 / 0.606         0.612 / 0.765         0.660 / 5.543         0.660 / 5.543       ms\n" +
                        "[7.555s]       Phase: Pause Mark Start                              4.459 / 4.459         4.636 / 6.500         6.160 / 547.572       6.160 / 547.572     ms\n" +
                        "[7.555s]       Phase: Pause Relocate Start                          3.791 / 3.791         3.970 / 5.443         4.047 / 8.993         4.047 / 8.993       ms\n" +
                        "[7.555s]    Subphase: Concurrent Mark                             306.253 / 306.593     303.509 / 452.030     254.759 / 601.564     254.759 / 601.564     ms\n" +
                        "[7.555s]    Subphase: Concurrent Mark Idle                          1.069 / 1.110         1.527 / 18.317        1.101 / 18.317        1.101 / 18.317      ms\n" +
                        "[7.555s]    Subphase: Concurrent Mark Try Flush                     0.554 / 0.685         0.872 / 18.247        0.507 / 18.247        0.507 / 18.247      ms\n" +
                        "[7.555s]    Subphase: Concurrent Mark Try Terminate                 0.978 / 1.112         1.386 / 18.318        0.998 / 18.318        0.998 / 18.318      ms\n" +
                        "[7.555s]    Subphase: Concurrent References Enqueue                 0.007 / 0.007         0.008 / 0.013         0.009 / 0.037         0.009 / 0.037       ms\n" +
                        "[7.555s]    Subphase: Concurrent References Process                 0.628 / 0.628         0.638 / 1.153         0.596 / 1.789         0.596 / 1.789       ms\n" +
                        "[7.555s]    Subphase: Concurrent Weak Roots                         0.497 / 0.618         0.492 / 0.670         0.502 / 1.001         0.502 / 1.001       ms\n" +
                        "[7.555s]    Subphase: Concurrent Weak Roots JNIWeakHandles          0.001 / 0.001         0.001 / 0.006         0.001 / 0.007         0.001 / 0.007       ms\n" +
                        "[7.555s]    Subphase: Concurrent Weak Roots StringTable             0.476 / 0.492         0.402 / 0.523         0.400 / 0.809         0.400 / 0.809       ms\n" +
                        "[7.555s]    Subphase: Concurrent Weak Roots VMWeakHandles           0.105 / 0.123         0.098 / 0.150         0.103 / 0.903         0.103 / 0.903       ms\n" +
                        "[7.555s]    Subphase: Pause Mark Try Complete                       0.000 / 0.000         0.001 / 0.004         0.156 / 1.063         0.156 / 1.063       ms\n" +
                        "[7.555s]    Subphase: Pause Remap TLABS                             0.040 / 0.040         0.046 / 0.073         0.050 / 0.140         0.050 / 0.140       ms\n" +
                        "[7.555s]    Subphase: Pause Retire TLABS                            0.722 / 0.722         0.835 / 1.689         0.754 / 1.919         0.754 / 1.919       ms\n" +
                        "[7.555s]    Subphase: Pause Roots                                   1.581 / 2.896         1.563 / 3.787         1.592 / 545.902       1.592 / 545.902     ms\n" +
                        "[7.555s]    Subphase: Pause Roots ClassLoaderDataGraph              1.461 / 2.857         1.549 / 3.782         1.554 / 6.380         1.554 / 6.380       ms\n" +
                        "[7.555s]    Subphase: Pause Roots CodeCache                         1.130 / 1.312         0.999 / 1.556         0.988 / 6.322         0.988 / 6.322       ms\n" +
                        "[7.555s]    Subphase: Pause Roots JNIHandles                        0.010 / 0.015         0.004 / 0.028         0.005 / 1.709         0.005 / 1.709       ms\n" +
                        "[7.555s]    Subphase: Pause Roots JNIWeakHandles                    0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[7.555s]    Subphase: Pause Roots JRFWeak                           0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[7.555s]    Subphase: Pause Roots JVMTIExport                       0.001 / 0.001         0.001 / 0.003         0.001 / 0.005         0.001 / 0.005       ms\n" +
                        "[7.555s]    Subphase: Pause Roots JVMTIWeakExport                   0.001 / 0.001         0.001 / 0.001         0.001 / 0.012         0.001 / 0.012       ms\n" +
                        "[7.555s]    Subphase: Pause Roots Management                        0.002 / 0.002         0.003 / 0.006         0.003 / 0.305         0.003 / 0.305       ms\n" +
                        "[7.555s]    Subphase: Pause Roots ObjectSynchronizer                0.000 / 0.000         0.000 / 0.001         0.000 / 0.006         0.000 / 0.006       ms\n" +
                        "[7.555s]    Subphase: Pause Roots Setup                             0.474 / 0.732         0.582 / 1.791         0.526 / 2.610         0.526 / 2.610       ms\n" +
                        "[7.555s]    Subphase: Pause Roots StringTable                       0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[7.555s]    Subphase: Pause Roots SystemDictionary                  0.028 / 0.039         0.027 / 0.075         0.033 / 2.777         0.033 / 2.777       ms\n" +
                        "[7.555s]    Subphase: Pause Roots Teardown                          0.003 / 0.005         0.003 / 0.009         0.003 / 0.035         0.003 / 0.035       ms\n" +
                        "[7.555s]    Subphase: Pause Roots Threads                           0.262 / 1.237         0.309 / 1.791         0.358 / 544.610       0.358 / 544.610     ms\n" +
                        "[7.555s]    Subphase: Pause Roots Universe                          0.003 / 0.004         0.003 / 0.009         0.003 / 0.047         0.003 / 0.047       ms\n" +
                        "[7.555s]    Subphase: Pause Roots VMWeakHandles                     0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[7.555s]    Subphase: Pause Weak Roots                              0.000 / 0.003         0.000 / 0.007         0.000 / 0.020         0.000 / 0.020       ms\n" +
                        "[7.555s]    Subphase: Pause Weak Roots JFRWeak                      0.001 / 0.001         0.001 / 0.002         0.001 / 0.012         0.001 / 0.012       ms\n" +
                        "[7.555s]    Subphase: Pause Weak Roots JNIWeakHandles               0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[7.555s]    Subphase: Pause Weak Roots JVMTIWeakExport              0.001 / 0.001         0.001 / 0.001         0.001 / 0.008         0.001 / 0.008       ms\n" +
                        "[7.555s]    Subphase: Pause Weak Roots Setup                        0.000 / 0.000         0.000 / 0.000         0.000 / 0.001         0.000 / 0.001       ms\n" +
                        "[7.555s]    Subphase: Pause Weak Roots StringTable                  0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[7.555s]    Subphase: Pause Weak Roots SymbolTable                  0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[7.555s]    Subphase: Pause Weak Roots Teardown                     0.001 / 0.001         0.001 / 0.001         0.001 / 0.015         0.001 / 0.015       ms\n" +
                        "[7.555s]    Subphase: Pause Weak Roots VMWeakHandles                0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[7.555s]      System: Java Threads                                    911 / 911             910 / 911             901 / 913             901 / 913         threads\n" +
                        "[7.555s] =========================================================================================================================================================\n" +
                        "[7.777s] Allocation Stall (ThreadPoolTaskScheduler-1) 0.204ms\n" +
                        "[7.888s] Allocation Stall (NioProcessor-2) 0.391ms\n" +
                        "[7.889s] Out Of Memory (thread 8)";
        UnifiedZGCLogParser parser = (UnifiedZGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));
        ZGCModel model = (ZGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 1);
        GCEvent gc = model.getGcEvents().get(0);
        Assert.assertEquals(gc.getGcid(), 374);
        Assert.assertEquals(gc.getStartTime(), 7000, DELTA);
        Assert.assertEquals(gc.getEndTime(), 7356, DELTA);
        Assert.assertEquals(gc.getDuration(), 356, DELTA);
        Assert.assertEquals(gc.getEventType(), GCEventType.ZGC_GARBAGE_COLLECTION);
        Assert.assertEquals(gc.getCause(), PROACTIVE);
        Assert.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getEndTime(), 7006, DELTA);
        Assert.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getDuration(), 4.459, DELTA);
        Assert.assertEquals(gc.getMemoryItem(METASPACE).getPostCapacity(), 128 * 1024 * 1024);
        Assert.assertEquals(gc.getMemoryItem(METASPACE).getPostUsed(), 125 * 1024 * 1024);
        Assert.assertEquals(gc.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 5614L * 1024 * 1024, 40960L * 1024 * 1024, 1454 * 1024 * 1024, 40960L * 1024 * 1024));
        Assert.assertEquals(gc.getAllocation(), 202 * 1024 * 1024);
        Assert.assertEquals(gc.getReclamation(), 4200L * 1024 * 1024);

        List<ZGCModel.ZStatistics> statistics = model.getStatistics();
        Assert.assertEquals(statistics.size(), 1);
        Assert.assertEquals(72, statistics.get(0).getStatisticItems().size());
        Assert.assertEquals(statistics.get(0).getStartTime(), 7555, DELTA);
        Assert.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10s(), 911, DELTA);
        Assert.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10h(), 913, DELTA);
        List<GCEvent> allocationStalls = model.getAllocationStalls();
        Assert.assertEquals(allocationStalls.size(), 2);
        Assert.assertEquals(allocationStalls.get(1).getEndTime(), 7888, DELTA);
        Assert.assertEquals(allocationStalls.get(1).getDuration(), 0.391, DELTA);
        Assert.assertEquals(((ThreadEvent) (allocationStalls.get(1))).getThreadName(), "NioProcessor-2");

        Assert.assertEquals(model.getOoms().size(), 1);
        ThreadEvent oom = model.getOoms().get(0);
        Assert.assertEquals(oom.getStartTime(), 7889, DELTA);
        Assert.assertEquals(oom.getThreadName(), "thread 8");
    }

    @Test
    public void testJDK8CMSParser() throws Exception {
        String log =
                "OpenJDK 64-Bit Server VM (25.212-b469) for linux-amd64 JRE (1.8.0_212-b469), built on Jun 16 2019 15:54:49 by \"admin\" with gcc 4.8.2\n" +
                        "Memory: 4k page, physical 8388608k(5632076k free), swap 0k(0k free)\n" +
                        "610.956: [Full GC (Heap Dump Initiated GC) 610.956: [CMS[YG occupancy: 1212954 K (1843200 K)]611.637: [weak refs processing, 0.0018945 secs]611.639: [class unloading, 0.0454119 secs]611.684: [scrub symbol table, 0.0248340 secs]611.709: [scrub string table, 0.0033967 secs]: 324459K->175339K(3072000K), 1.0268069 secs] 1537414K->1388294K(4915200K), [Metaspace: 114217K->113775K(1153024K)], 1.0277002 secs] [Times: user=1.71 sys=0.05, real=1.03 secs]\n" +
                        "674.686: [GC (Allocation Failure) 674.687: [ParNew: 1922432K->174720K(1922432K), 0.1691241 secs] 3557775K->1858067K(4019584K), 0.1706065 secs] [Times: user=0.54 sys=0.04, real=0.17 secs]\n" +
                        "675.110: Total time for which application threads were stopped: 0.0001215 seconds, Stopping threads took: 0.0000271 seconds\n" +
                        "675.111: Application time: 0.0170944 seconds\n" +
                        "675.164: [GC (CMS Initial Mark) [1 CMS-initial-mark: 1683347K(2097152K)] 1880341K(4019584K), 0.0714398 secs] [Times: user=0.19 sys=0.05, real=0.07 secs]" +
                        "675.461: [CMS-concurrent-mark-start]\n" +
                        "705.287: [GC (Allocation Failure) 705.288: [ParNew: 1922432K->174720K(1922432K), 0.2481441 secs] 3680909K->2051729K(4019584K), 0.2502404 secs] [Times: user=0.93 sys=0.10, real=0.25 secs]\n" +
                        "709.876: [CMS-concurrent-mark: 17.528/34.415 secs] [Times: user=154.39 sys=4.20, real=34.42 secs]\n" +
                        "709.959: [CMS-concurrent-preclean-start]\n" +
                        "710.570: [CMS-concurrent-preclean: 0.576/0.611 secs] [Times: user=3.08 sys=0.05, real=0.69 secs]\n" +
                        "710.571: [CMS-concurrent-abortable-preclean-start]\n" +
                        "715.691: [GC (Allocation Failure) 715.692: [ParNew: 1922432K->174720K(1922432K), 0.1974709 secs] 3799441K->2119132K(4019584K), 0.1992381 secs] [Times: user=0.61 sys=0.04, real=0.20 secs]\n" +
                        "717.759: [CMS-concurrent-abortable-preclean: 5.948/7.094 secs] [Times: user=32.21 sys=0.66, real=7.19 secs]\n" +
                        "717.792: [GC (CMS Final Remark) [YG occupancy: 438765 K (1922432 K)]717.792: [Rescan (parallel) , 0.1330457 secs]717.925: [weak refs processing, 0.0007103 secs]717.926: [class unloading, 0.2074917 secs]718.134: [scrub symbol table, 0.0751664 secs]718.209: [scrub string table, 0.0137015 secs][1 CMS-remark: 1944412K(2097152K)] 2383178K(4019584K), 0.4315000 secs] [Times: user=0.77 sys=0.01, real=0.43 secs]\n" +
                        "718.226: [CMS-concurrent-sweep-start]\n" +
                        "724.991: [GC (Allocation Failure) 724.992: [ParNew: 1922432K->174720K(1922432K), 0.2272846 secs] 3377417K->1710595K(4019584K), 0.2289948 secs] [Times: user=0.70 sys=0.01, real=0.23 secs]\n" +
                        "728.865: [CMS-concurrent-sweep: 8.279/10.639 secs] [Times: user=48.12 sys=1.21, real=10.64 secs]\n" +
                        "731.570: [CMS-concurrent-reset-start]\n" +
                        "731.806: [CMS-concurrent-reset: 0.205/0.237 secs] [Times: user=1.43 sys=0.04, real=0.34 secs]\n" +
                        "778.294: [GC (Allocation Failure) 778.295: [ParNew: 1922432K->163342K(1922432K), 0.2104952 secs] 3570857K->1917247K(4019584K), 0.2120639 secs] [Times: user=0.63 sys=0.00, real=0.21 secs]\n" +
                        "778.534: [GC (CMS Initial Mark) [1 CMS-initial-mark: 1753905K(2097152K)] 1917298K(4019584K), 0.0645754 secs] [Times: user=0.20 sys=0.01, real=0.06 secs]\n" +
                        "778.601: [CMS-concurrent-mark-start]\n" +
                        "792.762: [CMS-concurrent-mark: 11.404/14.161 secs] [Times: user=61.30 sys=2.27, real=14.17 secs]\n" +
                        "792.763: [CMS-concurrent-preclean-start]\n" +
                        "795.862: [CMS-concurrent-preclean: 2.148/3.100 secs] [Times: user=12.43 sys=0.91, real=3.10 secs]\n" +
                        "795.864: [CMS-concurrent-abortable-preclean-start]\n" +
                        "795.864: [CMS-concurrent-abortable-preclean: 0.000/0.000 secs] [Times: user=0.03 sys=0.00, real=0.00 secs]\n" +
                        "795.886: [GC (CMS Final Remark) [YG occupancy: 1619303 K (1922432 K)]795.887: [Rescan (parallel) , 0.2995817 secs]796.186: [weak refs processing, 0.0001985 secs]796.187: [class unloading, 0.1856105 secs]796.372: [scrub symbol table, 0.0734544 secs]796.446: [scrub string table, 0.0079670 secs][1 CMS-remark: 2048429K(2097152K)] 3667732K(4019584K), 0.5676600 secs] [Times: user=1.34 sys=0.01, real=0.57 secs]\n" +
                        "796.456: [CMS-concurrent-sweep-start]\n" +
                        "796.991: [GC (Allocation Failure) 796.992: [ParNew: 1922432K->1922432K(1922432K), 0.0000267 secs]796.992: [CMS797.832: [CMS-concurrent-sweep: 1.180/1.376 secs] [Times: user=3.42 sys=0.14, real=1.38 secs]\n" +
                        " (concurrent mode failure): 2034154K->1051300K(2097152K), 4.6146919 secs] 3956586K->1051300K(4019584K), [Metaspace: 296232K->296083K(1325056K)], 4.6165192 secs] [Times: user=4.60 sys=0.05, real=4.62 secs]\n" +
                        "813.396: [GC (Allocation Failure) 813.396: [ParNew813.404: [SoftReference, 4 refs, 0.0000260 secs]813.405: [WeakReference, 59 refs, 0.0000110 secs]813.406: [FinalReference, 1407 refs, 0.0025979 secs]813.407: [PhantomReference, 11 refs, 10 refs, 0.0000131 secs]813.408: [JNI Weak Reference, 0.0000088 secs]: 69952K->8704K(78656K), 0.0104509 secs] 69952K->11354K(253440K), 0.0105137 secs] [Times: user=0.04 sys=0.01, real=0.01 secs]\n";
        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        CMSGCModel model = (CMSGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 10);
        Assert.assertEquals(model.getSafepoints().size(), 1);

        Safepoint safepoint = model.getSafepoints().get(0);
        Assert.assertEquals(safepoint.getStartTime(), 675110 - 0.1215, DELTA);
        Assert.assertEquals(safepoint.getDuration(), 0.1215, DELTA);
        Assert.assertEquals(safepoint.getTimeToEnter(), 0.0271, DELTA);

        GCEvent fullgc = model.getGcEvents().get(0);
        Assert.assertEquals(fullgc.getStartTime(), 610956, DELTA);
        Assert.assertEquals(fullgc.getDuration(), 1027.7002, DELTA);
        Assert.assertEquals(fullgc.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullgc.getCause(), HEAP_DUMP);
        Assert.assertEquals(fullgc.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 114217 * 1024, 113775 * 1024, 1153024 * 1024));
        Assert.assertEquals(fullgc.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 1537414 * 1024, 1388294 * 1024, 4915200L * 1024));
        Assert.assertEquals(fullgc.getMemoryItem(OLD), new GCMemoryItem(OLD, 324459 * 1024, 175339 * 1024, 3072000L * 1024));
        Assert.assertEquals(fullgc.getPhases().size(), 4);
        Assert.assertEquals(fullgc.getLastPhaseOfType(GCEventType.WEAK_REFS_PROCESSING).getStartTime(), 611637, DELTA);
        Assert.assertEquals(fullgc.getLastPhaseOfType(GCEventType.WEAK_REFS_PROCESSING).getDuration(), 1.8945, DELTA);
        Assert.assertEquals(fullgc.getCpuTime().getUser(), 1710, DELTA);
        Assert.assertEquals(fullgc.getCpuTime().getSys(), 50, DELTA);
        Assert.assertEquals(fullgc.getCpuTime().getReal(), 1030, DELTA);

        fullgc = model.getGcEvents().get(8);
        Assert.assertEquals(fullgc.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullgc.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 3956586L * 1024, 1051300 * 1024, 4019584L * 1024));

        GCEvent youngGC = model.getGcEvents().get(9);
        Assert.assertEquals(youngGC.getStartTime(), 813396, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 10.5137, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 69952 * 1024, 11354 * 1024, 253440 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 69952 * 1024, 8704 * 1024, 78656 * 1024));
        Assert.assertNull(youngGC.getPhases());
        Assert.assertEquals(youngGC.getReferenceGC().getSoftReferenceStartTime(), 813404, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getSoftReferencePauseTime(), 0.0260, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getSoftReferenceCount(), 4);
        Assert.assertEquals(youngGC.getReferenceGC().getWeakReferenceStartTime(), 813405, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getWeakReferencePauseTime(), 0.0110, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getWeakReferenceCount(), 59);
        Assert.assertEquals(youngGC.getReferenceGC().getFinalReferenceStartTime(), 813406, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getFinalReferencePauseTime(), 2.5979, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getFinalReferenceCount(), 1407);
        Assert.assertEquals(youngGC.getReferenceGC().getPhantomReferenceStartTime(), 813407, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getPhantomReferencePauseTime(), 0.0131, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getPhantomReferenceCount(), 11);
        Assert.assertEquals(youngGC.getReferenceGC().getPhantomReferenceFreedCount(), 10);
        Assert.assertEquals(youngGC.getReferenceGC().getJniWeakReferenceStartTime(), 813408, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getJniWeakReferencePauseTime(), 0.0088, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getUser(), 40, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getSys(), 10, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 10, DELTA);

        GCEvent cms = model.getGcEvents().get(2);
        Assert.assertEquals(cms.getEventType(), GCEventType.CMS_CONCURRENT_MARK_SWEPT);
        Assert.assertEquals(cms.getStartTime(), 675164, DELTA);
        Assert.assertEquals(cms.getPhases().size(), 12, DELTA);
        for (GCEvent phase : cms.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_INITIAL_MARK).getStartTime(), 675164, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getDuration(), 34415, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getCpuTime().getUser(), 154390, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 770, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getDuration(), 431.5, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_RESET).getDuration(), 237, DELTA);
    }

    @Test
    public void testJDK8CMSCPUTime() throws Exception {
        String log = "2022-11-28T14:57:05.217+0800: 6.216: [GC (CMS Initial Mark) [1 CMS-initial-mark: 0K(3584000K)] 619320K(5519360K), 0.1236090 secs] [Times: user=0.08 sys=0.08, real=0.13 secs] \n" +
                "2022-11-28T14:57:05.341+0800: 6.340: [CMS-concurrent-mark-start]\n" +
                "2022-11-28T14:57:05.342+0800: 6.340: [CMS-concurrent-mark: 0.001/0.001 secs] [Times: user=0.01 sys=0.00, real=0.00 secs] \n" +
                "2022-11-28T14:57:05.342+0800: 6.340: [CMS-concurrent-preclean-start]\n" +
                "2022-11-28T14:57:05.347+0800: 6.345: [CMS-concurrent-preclean: 0.005/0.005 secs] [Times: user=0.01 sys=0.00, real=0.03 secs] \n" +
                "2022-11-28T14:57:05.347+0800: 6.346: [CMS-concurrent-abortable-preclean-start]\n" +
                "2022-11-28T14:57:09.974+0800: 10.973: [GC (Allocation Failure) 2022-11-28T14:57:09.974+0800: 10.973: [ParNew2022-11-28T14:57:09.997+0800: 10.996: [CMS-concurrent-abortable-preclean: 0.335/4.650 secs] [Times: user=10.64 sys=0.72, real=4.65 secs] \n" +
                ": 1720320K->36032K(1935360K), 0.0395605 secs] 1720320K->36032K(5519360K), 0.0397919 secs] [Times: user=0.18 sys=0.03, real=0.05 secs] \n" +
                "2022-11-28T14:57:10.015+0800: 11.013: [GC (CMS Final Remark) [YG occupancy: 70439 K (1935360 K)]2022-11-28T14:57:10.015+0800: 11.013: [Rescan (parallel) , 0.0049504 secs]2022-11-28T14:57:10.020+0800: 11.018: [weak refs processing, 0.0001257 secs]2022-11-28T14:57:10.020+0800: 11.018: [class unloading, 0.0154147 secs]2022-11-28T14:57:10.035+0800: 11.034: [scrub symbol table, 0.0077166 secs]2022-11-28T14:57:10.043+0800: 11.042: [scrub string table, 0.0006843 secs][1 CMS-remark: 0K(3584000K)] 70439K(5519360K), 0.0301977 secs] [Times: user=0.15 sys=0.00, real=0.03 secs] \n" +
                "2022-11-28T14:57:10.046+0800: 11.044: [CMS-concurrent-sweep-start]\n" +
                "2022-11-28T14:57:10.046+0800: 11.044: [CMS-concurrent-sweep: 0.000/0.000 secs] [Times: user=0.00 sys=0.00, real=0.02 secs] \n" +
                "2022-11-28T14:57:10.047+0800: 11.045: [CMS-concurrent-reset-start]\n" +
                "2022-11-28T14:57:10.074+0800: 11.072: [CMS-concurrent-reset: 0.027/0.027 secs] [Times: user=0.25 sys=0.04, real=0.04 secs] ";
        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        CMSGCModel model = (CMSGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_INITIAL_MARK).getCpuTime().getReal(),130, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_MARK).getCpuTime().getUser(),10, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_PRECLEAN).getCpuTime().getReal(),30, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_ABORTABLE_PRECLEAN).getCpuTime().getReal(),4650, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getReal(),30, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_SWEEP).getCpuTime().getReal(),20, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_RESET).getCpuTime().getReal(),40, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.YOUNG_GC).getCpuTime().getReal(),50, DELTA);
    }

    @Test
    public void testJDK8G1GCParser() throws Exception {
        String log = "3.960: [GC pause (G1 Evacuation Pause) (young)4.000: [SoftReference, 0 refs, 0.0000435 secs]4.000: [WeakReference, 374 refs, 0.0002082 secs]4.001: [FinalReference, 5466 refs, 0.0141707 secs]4.015: [PhantomReference, 0 refs, 0 refs, 0.0000253 secs]4.015: [JNI Weak Reference, 0.0000057 secs], 0.0563085 secs]\n" +
                "   [Parallel Time: 39.7 ms, GC Workers: 4]\n" +
                "      [GC Worker Start (ms): Min: 3959.8, Avg: 3959.9, Max: 3960.1, Diff: 0.2]\n" +
                "      [Ext Root Scanning (ms): Min: 2.6, Avg: 10.1, Max: 17.9, Diff: 15.2, Sum: 40.4]\n" +
                "      [Update RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]\n" +
                "         [Processed Buffers: Min: 0, Avg: 0.0, Max: 0, Diff: 0, Sum: 0]\n" +
                "      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]\n" +
                "      [Code Root Scanning (ms): Min: 0.0, Avg: 0.5, Max: 2.1, Diff: 2.1, Sum: 2.1]\n" +
                "      [Object Copy (ms): Min: 18.1, Avg: 26.2, Max: 33.7, Diff: 15.6, Sum: 104.9]\n" +
                "      [Termination (ms): Min: 0.0, Avg: 1.5, Max: 3.5, Diff: 3.5, Sum: 6.2]\n" +
                "         [Termination Attempts: Min: 1, Avg: 21.8, Max: 51, Diff: 50, Sum: 87]\n" +
                "      [GC Worker Other (ms): Min: 0.0, Avg: 0.1, Max: 0.1, Diff: 0.0, Sum: 0.2]\n" +
                "      [GC Worker Total (ms): Min: 38.0, Avg: 38.5, Max: 39.5, Diff: 1.5, Sum: 153.8]\n" +
                "      [GC Worker End (ms): Min: 3998.0, Avg: 3998.4, Max: 3999.4, Diff: 1.4]\n" +
                "   [Code Root Fixup: 0.2 ms]\n" +
                "   [Code Root Purge: 0.2 ms]\n" +
                "   [Clear CT: 0.2 ms]\n" +
                "   [Other: 16.0 ms]\n" +
                "      [Choose CSet: 0.0 ms]\n" +
                "      [Ref Proc: 15.1 ms]\n" +
                "      [Ref Enq: 0.2 ms]\n" +
                "      [Redirty Cards: 0.1 ms]\n" +
                "      [Humongous Register: 0.0 ms]\n" +
                "      [Humongous Reclaim: 0.0 ms]\n" +
                "      [Free CSet: 0.3 ms]\n" +
                "   [Eden: 184.0M(184.0M)->0.0B(160.0M) Survivors: 0.0B->24.0M Heap: 184.0M(3800.0M)->19.3M(3800.0M)]\n" +
                " [Times: user=0.07 sys=0.01, real=0.06 secs]\n" +
                "4.230: [GC concurrent-root-region-scan-start]\n" +
                "4.391: [GC concurrent-root-region-scan-end, 0.1608430 secs]\n" +
                "4.391: [GC concurrent-mark-start]\n" +
                "7.101: [GC concurrent-mark-reset-for-overflow]\n" +
                "19.072: [GC concurrent-mark-end, 14.6803750 secs]\n" +
                "19.078: [GC remark 19.078: [Finalize Marking, 0.1774665 secs] 19.255: [GC ref-proc, 0.1648116 secs] 19.420: [Unloading, 0.1221964 secs], 0.4785858 secs]\n" +
                " [Times: user=1.47 sys=0.31, real=0.48 secs]\n" +
                "19.563: [GC cleanup 11G->9863M(20G), 0.0659638 secs]\n" +
                " [Times: user=0.20 sys=0.01, real=0.07 secs]\n" +
                "19.630: [GC concurrent-cleanup-start]\n" +
                "19.631: [GC concurrent-cleanup-end, 0.0010377 secs]\n" +
                "23.346: [Full GC (Metadata GC Threshold)  7521M->7002M(46144M), 1.9242692 secs]\n" +
                "   [Eden: 0.0B(1760.0M)->0.0B(2304.0M) Survivors: 544.0M->0.0B Heap: 7521.7M(46144.0M)->7002.8M(46144.0M)], [Metaspace: 1792694K->291615K(698368K)]\n" +
                " [Times: user=2.09 sys=0.19, real=1.92 secs]\n" +
                "79.619: [GC pause (G1 Evacuation Pause) (mixed)79.636: [SoftReference, 1 refs, 0.0000415 secs]79.636: [WeakReference, 2 refs, 0.0000061 secs]79.636: [FinalReference, 3 refs, 0.0000049 secs]79.636: [PhantomReference, 4 refs, 5 refs, 0.0000052 secs]79.636: [JNI Weak Reference, 0.0000117 secs] (to-space exhausted), 0.0264971 secs]\n" +
                "   [Parallel Time: 20.5 ms, GC Workers: 4]\n" +
                "      [GC Worker Start (ms): Min: 1398294.3, Avg: 1398294.4, Max: 1398294.5, Diff: 0.2]\n" +
                "      [Ext Root Scanning (ms): Min: 1.8, Avg: 2.0, Max: 2.2, Diff: 0.4, Sum: 15.7]\n" +
                "      [Update RS (ms): Min: 1.2, Avg: 1.5, Max: 1.7, Diff: 0.5, Sum: 11.8]\n" +
                "         [Processed Buffers: Min: 21, Avg: 27.0, Max: 30, Diff: 9, Sum: 216]\n" +
                "      [Scan RS (ms): Min: 1.8, Avg: 1.9, Max: 2.2, Diff: 0.4, Sum: 15.5]\n" +
                "      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]\n" +
                "      [Object Copy (ms): Min: 14.5, Avg: 14.7, Max: 14.9, Diff: 0.4, Sum: 118.0]\n" +
                "      [Termination (ms): Min: 0.0, Avg: 0.1, Max: 0.1, Diff: 0.1, Sum: 0.5]\n" +
                "         [Termination Attempts: Min: 1, Avg: 148.2, Max: 181, Diff: 180, Sum: 1186]\n" +
                "      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.1, Diff: 0.0, Sum: 0.3]\n" +
                "      [GC Worker Total (ms): Min: 20.1, Avg: 20.2, Max: 20.3, Diff: 0.2, Sum: 161.9]\n" +
                "      [GC Worker End (ms): Min: 1398314.7, Avg: 1398314.7, Max: 1398314.7, Diff: 0.0]\n" +
                "   [Code Root Fixup: 0.0 ms]\n" +
                "   [Code Root Purge: 0.0 ms]\n" +
                "   [Clear CT: 0.5 ms]\n" +
                "   [Other: 10.4 ms]\n" +
                "      [Choose CSet: 0.0 ms]\n" +
                "      [Ref Proc: 8.8 ms]\n" +
                "      [Ref Enq: 0.3 ms]\n" +
                "      [Redirty Cards: 0.2 ms]\n" +
                "      [Free CSet: 0.1 ms]\n" +
                "   [Eden: 2304.0M(2304.0M)->0.0B(2304.0M) Survivors: 192.0M->192.0M Heap: 15.0G(19.8G)->12.8G(19.8G)]\n" +
                " [Times: user=0.17 sys=0.00, real=0.03 secs]";
        PreUnifiedG1GCLogParser parser = (PreUnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        G1GCModel model = (G1GCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 4);
        Assert.assertEquals(model.getParallelThread(), 4);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getStartTime(), 3960, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 56.3085, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), G1_EVACUATION_PAUSE);
        Assert.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 184 * 1024 * 1024, 3800L * 1024 * 1024, (int) (19.3 * 1024 * 1024), 3800L * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 184 * 1024 * 1024, 184 * 1024 * 1024, 0, 160 * 1024 * 1024));
        Assert.assertNotNull(youngGC.getPhases());
        for (GCEvent phase : youngGC.getPhases()) {
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(youngGC.getLastPhaseOfType(GCEventType.G1_GC_REFPROC).getDuration(), 15.1, DELTA);
        Assert.assertEquals(youngGC.getLastPhaseOfType(GCEventType.G1_CODE_ROOT_SCANNING).getDuration(), 0.5, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getSoftReferenceStartTime(), 4000, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getJniWeakReferencePauseTime(), 0.0057, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getUser(), 70, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getSys(), 10, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 60, DELTA);

        GCEvent concurrentCycle = model.getGcEvents().get(1);
        Assert.assertEquals(concurrentCycle.getStartTime(), 4230, DELTA);
        Assert.assertEquals(concurrentCycle.getPhases().size(), 9);
        for (GCEvent phase : concurrentCycle.getPhases()) {
            if (phase.getEventType() != GCEventType.G1_CONCURRENT_MARK_RESET_FOR_OVERFLOW) {
                Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
            }
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_CONCURRENT_SCAN_ROOT_REGIONS).getStartTime(), 4230, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_CONCURRENT_SCAN_ROOT_REGIONS).getDuration(), 160.8430, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_REMARK).getStartTime(), 19078, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_REMARK).getDuration(), 478.5858, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_PAUSE_CLEANUP).getMemoryItem(HEAP).getPostUsed(), 9863L * 1024 * 1024, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_REMARK).getCpuTime().getUser(), 1470, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_PAUSE_CLEANUP).getCpuTime().getSys(), 10, DELTA);

        GCEvent fullGC = model.getGcEvents().get(2);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getStartTime(), 23346, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 1924.2692, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), METADATA_GENERATION_THRESHOLD);
        Assert.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 1792694 * 1024, 291615 * 1024, 698368 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, (long) (7521.7 * 1024 * 1024), (long) (46144.0 * 1024 * 1024), (long) (7002.8 * 1024 * 1024), (long) (46144.0 * 1024 * 1024)));
        Assert.assertEquals(fullGC.getCpuTime().getUser(), 2090, DELTA);
        Assert.assertEquals(fullGC.getCpuTime().getSys(), 190, DELTA);
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 1920, DELTA);

        GCEvent mixedGC = model.getGcEvents().get(3);
        Assert.assertEquals(mixedGC.getStartTime(), 79619, DELTA);
        Assert.assertEquals(mixedGC.getDuration(), 26.4971, DELTA);
        Assert.assertEquals(mixedGC.getEventType(), GCEventType.G1_MIXED_GC);
        Assert.assertEquals(mixedGC.getCause(), G1_EVACUATION_PAUSE);
        Assert.assertTrue(mixedGC.isTrue(TO_SPACE_EXHAUSTED));
        Assert.assertEquals(mixedGC.getMemoryItem(HEAP).getPostCapacity(), (long) (19.8 * 1024 * 1024 * 1024));
        Assert.assertEquals(mixedGC.getMemoryItem(EDEN).getPreUsed(), 2304L * 1024 * 1024);
        Assert.assertNotNull(mixedGC.getPhases());
        for (GCEvent phase : mixedGC.getPhases()) {
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }

        GCLogMetadata metadata = model.getGcModelMetadata();
        Assert.assertFalse(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS.getName()));
        Assert.assertFalse(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_UNDO_CYCLE.getName()));
    }

    @Test
    public void testJDK8G1GCParserAdaptiveSize() throws Exception {
        // although we don't read anything from options like -XX:+PrintAdaptiveSizePolicy, they should not
        // affect parsing
        String log = "2022-02-09T15:55:55.807+0800: 0.683: [GC pause (G1 Evacuation Pause) (young)\n" +
                "Desired survivor size 3670016 bytes, new threshold 15 (max 15)\n" +
                " 0.683: [G1Ergonomics (CSet Construction) start choosing CSet, _pending_cards: 0, predicted base time: 10.00 ms, remaining time: 240.00 ms, target pause time: 250.00 ms]\n" +
                " 0.683: [G1Ergonomics (CSet Construction) add young regions to CSet, eden: 51 regions, survivors: 0 regions, predicted young region time: 1298.76 ms]\n" +
                " 0.683: [G1Ergonomics (CSet Construction) finish choosing CSet, eden: 51 regions, survivors: 0 regions, old: 0 regions, predicted pause time: 1308.76 ms, target pause time: 250.00 ms]\n" +
                ", 0.0085898 secs]\n" +
                "   [Parallel Time: 5.5 ms, GC Workers: 4]\n" +
                "      [GC Worker Start (ms): Min: 682.6, Avg: 682.6, Max: 682.7, Diff: 0.0]\n" +
                "      [Ext Root Scanning (ms): Min: 0.8, Avg: 1.2, Max: 1.6, Diff: 0.8, Sum: 4.8]\n" +
                "      [Update RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]\n" +
                "         [Processed Buffers: Min: 0, Avg: 0.0, Max: 0, Diff: 0, Sum: 0]\n" +
                "      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]\n" +
                "      [Code Root Scanning (ms): Min: 0.0, Avg: 0.2, Max: 0.9, Diff: 0.9, Sum: 0.9]\n" +
                "      [Object Copy (ms): Min: 3.5, Avg: 3.9, Max: 4.5, Diff: 1.0, Sum: 15.7]\n" +
                "      [Termination (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]\n" +
                "         [Termination Attempts: Min: 1, Avg: 6.8, Max: 9, Diff: 8, Sum: 27]\n" +
                "      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]\n" +
                "      [GC Worker Total (ms): Min: 5.4, Avg: 5.4, Max: 5.4, Diff: 0.0, Sum: 21.6]\n" +
                "      [GC Worker End (ms): Min: 688.1, Avg: 688.1, Max: 688.1, Diff: 0.0]\n" +
                "   [Code Root Fixup: 0.0 ms]\n" +
                "   [Code Root Purge: 0.0 ms]\n" +
                "   [Clear CT: 0.1 ms]\n" +
                "   [Other: 3.0 ms]\n" +
                "      [Choose CSet: 0.0 ms]\n" +
                "      [Ref Proc: 2.6 ms]\n" +
                "      [Ref Enq: 0.0 ms]\n" +
                "      [Redirty Cards: 0.1 ms]\n" +
                "      [Humongous Register: 0.0 ms]\n" +
                "      [Humongous Reclaim: 0.0 ms]\n" +
                "      [Free CSet: 0.1 ms]\n" +
                "   [Eden: 52224.0K(52224.0K)->0.0B(45056.0K) Survivors: 0.0B->7168.0K Heap: 52224.0K(1024.0M)->8184.0K(1024.0M)]\n" +
                " [Times: user=0.02 sys=0.01, real=0.01 secs] ";
        PreUnifiedG1GCLogParser parser = (PreUnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        G1GCModel model = (G1GCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 1);
        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getStartTime(), 683, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 8.5898, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), G1_EVACUATION_PAUSE);
        Assert.assertEquals(youngGC.getMemoryItem(HEAP).getPreUsed(), 52224 * 1024);
    }

    @Test
    public void testJDK11SerialGCParser() throws Exception {
        String log = "[0.486s][info][gc,start     ] GC(0) Pause Young (Allocation Failure)\n" +
                "[0.511s][info][gc,heap      ] GC(0) DefNew: 69952K->8704K(78656K)\n" +
                "[0.511s][info][gc,heap      ] GC(0) Tenured: 0K->24185K(174784K)\n" +
                "[0.511s][info][gc,metaspace ] GC(0) Metaspace: 6529K->6519K(1056768K)\n" +
                "[0.511s][info][gc           ] GC(0) Pause Young (Allocation Failure) 68M->32M(247M) 25.164ms\n" +
                "[0.511s][info][gc,cpu       ] GC(0) User=0.02s Sys=0.00s Real=0.02s\n" +
                "[5.614s][info][gc,start     ] GC(1) Pause Full (Allocation Failure)\n" +
                "[5.614s][info][gc,phases,start] GC(1) Phase 1: Mark live objects\n" +
                "[5.662s][info][gc,phases      ] GC(1) Phase 1: Mark live objects 47.589ms\n" +
                "[5.662s][info][gc,phases,start] GC(1) Phase 2: Compute new object addresses\n" +
                "[5.688s][info][gc,phases      ] GC(1) Phase 2: Compute new object addresses 26.097ms\n" +
                "[5.688s][info][gc,phases,start] GC(1) Phase 3: Adjust pointers\n" +
                "[5.743s][info][gc,phases      ] GC(1) Phase 3: Adjust pointers 55.459ms\n" +
                "[5.743s][info][gc,phases,start] GC(1) Phase 4: Move objects\n" +
                "[5.760s][info][gc,phases      ] GC(1) Phase 4: Move objects 17.259ms\n" +
                "[5.761s][info][gc             ] GC(1) Pause Full (Allocation Failure) 215M->132M(247M) 146.617ms";
        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        SerialGCModel model = (SerialGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 2);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 486, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 25.164, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 69952 * 1024, 8704 * 1024, 78656 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, 24185 * 1024, 174784 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 6529 * 1024, 6519 * 1024, 1056768 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 68 * 1024 * 1024, 32 * 1024 * 1024, 247 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 20, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assert.assertEquals(fullGC.getGcid(), 1);
        Assert.assertEquals(fullGC.getStartTime(), 5614, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 146.617, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 215 * 1024 * 1024, 132 * 1024 * 1024, 247 * 1024 * 1024));
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 26.097, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 17.259, DELTA);
    }

    @Test
    public void testJDK11ParallelGCParser() throws Exception {
        String log = "[0.455s][info][gc,start     ] GC(0) Pause Young (Allocation Failure)\n" +
                "[0.466s][info][gc,heap      ] GC(0) PSYoungGen: 65536K->10720K(76288K)\n" +
                "[0.466s][info][gc,heap      ] GC(0) ParOldGen: 0K->20800K(175104K)\n" +
                "[0.466s][info][gc,metaspace ] GC(0) Metaspace: 6531K->6531K(1056768K)\n" +
                "[0.466s][info][gc           ] GC(0) Pause Young (Allocation Failure) 64M->30M(245M) 11.081ms\n" +
                "[0.466s][info][gc,cpu       ] GC(0) User=0.03s Sys=0.02s Real=0.01s\n" +
                "[2.836s][info][gc,start     ] GC(1) Pause Full (Ergonomics)\n" +
                "[2.836s][info][gc,phases,start] GC(1) Marking Phase\n" +
                "[2.857s][info][gc,phases      ] GC(1) Marking Phase 21.145ms\n" +
                "[2.857s][info][gc,phases,start] GC(1) Summary Phase\n" +
                "[2.857s][info][gc,phases      ] GC(1) Summary Phase 0.006ms\n" +
                "[2.857s][info][gc,phases,start] GC(1) Adjust Roots\n" +
                "[2.859s][info][gc,phases      ] GC(1) Adjust Roots 1.757ms\n" +
                "[2.859s][info][gc,phases,start] GC(1) Compaction Phase\n" +
                "[2.881s][info][gc,phases      ] GC(1) Compaction Phase 22.465ms\n" +
                "[2.881s][info][gc,phases,start] GC(1) Post Compact\n" +
                "[2.882s][info][gc,phases      ] GC(1) Post Compact 1.054ms\n" +
                "[2.882s][info][gc,heap        ] GC(1) PSYoungGen: 10729K->0K(76288K)\n" +
                "[2.882s][info][gc,heap        ] GC(1) ParOldGen: 141664K->94858K(175104K)\n" +
                "[2.882s][info][gc,metaspace   ] GC(1) Metaspace: 7459K->7459K(1056768K)\n" +
                "[2.882s][info][gc             ] GC(1) Pause Full (Ergonomics) 148M->92M(245M) 46.539ms\n" +
                "[2.882s][info][gc,cpu         ] GC(1) User=0.17s Sys=0.00s Real=0.05s";
        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        ParallelGCModel model = (ParallelGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 2);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 455, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 11.081, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 65536 * 1024, 10720 * 1024, 76288 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, 20800 * 1024, 175104 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 6531 * 1024, 6531 * 1024, 1056768 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 64 * 1024 * 1024, 30 * 1024 * 1024, 245 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 10, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assert.assertEquals(fullGC.getGcid(), 1);
        Assert.assertEquals(fullGC.getStartTime(), 2836, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 46.539, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), ERGONOMICS);
        Assert.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 10729 * 1024, 0, 76288 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 141664 * 1024, 94858 * 1024, 175104 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 7459 * 1024, 7459 * 1024, 1056768 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 148 * 1024 * 1024, 92 * 1024 * 1024, 245 * 1024 * 1024));
        Assert.assertEquals(fullGC.getPhases().size(), 5);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_SUMMARY).getDuration(), 0.006, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_COMPACTION).getDuration(), 22.465, DELTA);
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 50, DELTA);
    }

    @Test
    public void testJDK11CMSGCParser() throws Exception {
        String log = "[0.479s][info][gc,start     ] GC(0) Pause Young (Allocation Failure)\n" +
                "[0.480s][info][gc,task      ] GC(0) Using 5 workers of 8 for evacuation\n" +
                "[0.510s][info][gc,heap      ] GC(0) ParNew: 69952K->8703K(78656K)\n" +
                "[0.510s][info][gc,heap      ] GC(0) CMS: 0K->24072K(174784K)\n" +
                "[0.510s][info][gc,metaspace ] GC(0) Metaspace: 6531K->6530K(1056768K)\n" +
                "[0.510s][info][gc           ] GC(0) Pause Young (Allocation Failure) 68M->32M(247M) 31.208ms\n" +
                "[0.510s][info][gc,cpu       ] GC(0) User=0.06s Sys=0.03s Real=0.03s\n" +
                "[3.231s][info][gc,start     ] GC(1) Pause Initial Mark\n" +
                "[3.235s][info][gc           ] GC(1) Pause Initial Mark 147M->147M(247M) 3.236ms\n" +
                "[3.235s][info][gc,cpu       ] GC(1) User=0.01s Sys=0.02s Real=0.03s\n" +
                "[3.235s][info][gc           ] GC(1) Concurrent Mark\n" +
                "[3.235s][info][gc,task      ] GC(1) Using 2 workers of 2 for marking\n" +
                "[3.257s][info][gc           ] GC(1) Concurrent Mark 22.229ms\n" +
                "[3.257s][info][gc,cpu       ] GC(1) User=0.07s Sys=0.00s Real=0.03s\n" +
                "[3.257s][info][gc           ] GC(1) Concurrent Preclean\n" +
                "[3.257s][info][gc           ] GC(1) Concurrent Preclean 0.264ms\n" +
                "[3.257s][info][gc,cpu       ] GC(1) User=0.00s Sys=0.00s Real=0.00s\n" +
                "[3.257s][info][gc,start     ] GC(1) Pause Remark\n" +
                "[3.259s][info][gc           ] GC(1) Pause Remark 149M->149M(247M) 1.991ms\n" +
                "[3.259s][info][gc,cpu       ] GC(1) User=0.02s Sys=0.03s Real=0.01s\n" +
                "[3.259s][info][gc           ] GC(1) Concurrent Sweep\n" +
                "[3.279s][info][gc           ] GC(1) Concurrent Sweep 19.826ms\n" +
                "[3.279s][info][gc,cpu       ] GC(1) User=0.03s Sys=0.00s Real=0.02s\n" +
                "[3.279s][info][gc           ] GC(1) Concurrent Reset\n" +
                "[3.280s][info][gc           ] GC(1) Concurrent Reset 0.386ms\n" +
                "[3.280s][info][gc,cpu       ] GC(1) User=0.00s Sys=0.00s Real=0.00s\n" +
                "[3.280s][info][gc,heap      ] GC(1) Old: 142662K->92308K(174784K)\n" +
                "[8.970s][info][gc,start     ] GC(2) Pause Full (Allocation Failure)\n" +
                "[8.970s][info][gc,phases,start] GC(2) Phase 1: Mark live objects\n" +
                "[9.026s][info][gc,phases      ] GC(2) Phase 1: Mark live objects 55.761ms\n" +
                "[9.026s][info][gc,phases,start] GC(2) Phase 2: Compute new object addresses\n" +
                "[9.051s][info][gc,phases      ] GC(2) Phase 2: Compute new object addresses 24.761ms\n" +
                "[9.051s][info][gc,phases,start] GC(2) Phase 3: Adjust pointers\n" +
                "[9.121s][info][gc,phases      ] GC(2) Phase 3: Adjust pointers 69.678ms\n" +
                "[9.121s][info][gc,phases,start] GC(2) Phase 4: Move objects\n" +
                "[9.149s][info][gc,phases      ] GC(2) Phase 4: Move objects 28.069ms\n" +
                "[9.149s][info][gc             ] GC(2) Pause Full (Allocation Failure) 174M->166M(247M) 178.617ms\n" +
                "[9.149s][info][gc,cpu         ] GC(2) User=0.17s Sys=0.00s Real=0.18s";
        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        CMSGCModel model = (CMSGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 3);
        Assert.assertEquals(model.getParallelThread(), 8);
        Assert.assertEquals(model.getConcurrentThread(), 2);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 479, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 31.208, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 69952 * 1024, 8703 * 1024, 78656 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, 24072 * 1024, 174784 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 6531 * 1024, 6530 * 1024, 1056768 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 68 * 1024 * 1024, 32 * 1024 * 1024, 247 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 30, DELTA);

        GCEvent cms = model.getGcEvents().get(1);
        Assert.assertEquals(cms.getGcid(), 1);
        Assert.assertEquals(cms.getEventType(), GCEventType.CMS_CONCURRENT_MARK_SWEPT);
        Assert.assertEquals(cms.getStartTime(), 3231, DELTA);
        Assert.assertEquals(cms.getPhases().size(), 6);
        for (GCEvent phase : cms.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
            Assert.assertNotNull(phase.getCpuTime());
            if (phase.getEventType() == GCEventType.CMS_INITIAL_MARK || phase.getEventType() == GCEventType.CMS_FINAL_REMARK) {
                Assert.assertNotNull(phase.getMemoryItem(HEAP));
            }
        }
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_INITIAL_MARK).getStartTime(), 3231, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getDuration(), 22.229, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getCpuTime().getUser(), 70, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 20, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getDuration(), 1.991, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_RESET).getDuration(), 0.386, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_SWEEP).getMemoryItem(OLD), new GCMemoryItem(OLD, 142662 * 1024, 92308 * 1024, 174784 * 1024));

        GCEvent fullGC = model.getGcEvents().get(2);
        Assert.assertEquals(fullGC.getGcid(), 2);
        Assert.assertEquals(fullGC.getStartTime(), 8970, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 178.617, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 174 * 1024 * 1024, 166 * 1024 * 1024, 247 * 1024 * 1024));
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 180, DELTA);
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 24.761, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 28.069, DELTA);
    }

    @Test
    public void testJDK8ParallelGCParser() throws Exception {
        String log =
                "0.141: [GC (Allocation Failure) [PSYoungGen: 25145K->4077K(29696K)] 25145K->16357K(98304K), 0.0225874 secs] [Times: user=0.10 sys=0.01, real=0.03 secs]\n" +
                        "0.269: [Full GC (Ergonomics) [PSYoungGen: 4096K->0K(55296K)] [ParOldGen: 93741K->67372K(174592K)] 97837K->67372K(229888K), [Metaspace: 3202K->3202K(1056768K)], 0.6862093 secs] [Times: user=2.60 sys=0.02, real=0.69 secs]\n" +
                        "0.962: [GC (Allocation Failure) [PSYoungGen: 51200K->4096K(77824K)] 118572K->117625K(252416K), 0.0462864 secs] [Times: user=0.29 sys=0.01, real=0.05 secs]\n" +
                        "1.872: [Full GC (Ergonomics) [PSYoungGen: 4096K->0K(103936K)] [ParOldGen: 169794K->149708K(341504K)] 173890K->149708K(445440K), [Metaspace: 3202K->3202K(1056768K)], 1.3724621 secs] [Times: user=8.33 sys=0.01, real=1.38 secs]\n" +
                        "3.268: [GC (Allocation Failure) [PSYoungGen: 99840K->56802K(113664K)] 249548K->302089K(455168K), 0.1043993 secs] [Times: user=0.75 sys=0.06, real=0.10 secs]\n" +
                        "14.608: [Full GC (Ergonomics) [PSYoungGen: 65530K->0K(113664K)] [ParOldGen: 341228K->720K(302592K)] 406759K->720K(416256K), [Metaspace: 3740K->3737K(1056768K)], 0.0046781 secs] [Times: user=0.02 sys=0.01, real=0.00 secs]\n";
        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        ParallelGCModel model = (ParallelGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 6);

        GCEvent youngGC = model.getGcEvents().get(2);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getStartTime(), 962, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 46.2864, DELTA);
        GCMemoryItem youngGen = youngGC.getMemoryItem(YOUNG);
        Assert.assertEquals(youngGen.getPreUsed(), 51200 * 1024);
        Assert.assertEquals(youngGen.getPostUsed(), 4096 * 1024);
        Assert.assertEquals(youngGen.getPostCapacity(), 77824 * 1024);
        GCMemoryItem total = youngGC.getMemoryItem(HEAP);
        Assert.assertEquals(total.getPreUsed(), 118572 * 1024);
        Assert.assertEquals(total.getPostUsed(), 117625 * 1024);
        Assert.assertEquals(total.getPostCapacity(), 252416 * 1024);
        Assert.assertEquals(youngGC.getCpuTime().getUser(), 290, DELTA);

        GCEvent fullGC = model.getGcEvents().get(5);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), ERGONOMICS);
        Assert.assertEquals(fullGC.getStartTime(), 14608, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 4.6781, DELTA);
        youngGen = fullGC.getMemoryItem(YOUNG);
        Assert.assertEquals(youngGen.getPreUsed(), 65530 * 1024);
        Assert.assertEquals(youngGen.getPostUsed(), 0);
        Assert.assertEquals(youngGen.getPostCapacity(), 113664 * 1024);
        GCMemoryItem oldGen = fullGC.getMemoryItem(OLD);
        Assert.assertEquals(oldGen.getPreUsed(), 341228 * 1024);
        Assert.assertEquals(oldGen.getPostUsed(), 720 * 1024);
        Assert.assertEquals(oldGen.getPostCapacity(), 302592 * 1024);
        GCMemoryItem metaspace = fullGC.getMemoryItem(METASPACE);
        Assert.assertEquals(metaspace.getPreUsed(), 3740 * 1024);
        Assert.assertEquals(metaspace.getPostUsed(), 3737 * 1024);
        Assert.assertEquals(metaspace.getPostCapacity(), 1056768 * 1024);
        total = fullGC.getMemoryItem(HEAP);
        Assert.assertEquals(total.getPreUsed(), 406759 * 1024);
        Assert.assertEquals(total.getPostUsed(), 720 * 1024);
        Assert.assertEquals(total.getPostCapacity(), 416256 * 1024);
        Assert.assertEquals(fullGC.getCpuTime().getUser(), 20, DELTA);
    }

    @Test
    public void testJDK8SerialGCParser() throws Exception {
        String log =
                "2021-12-07T11:18:11.688+0800: #0: [GC (Allocation Failure) 2021-12-07T11:18:11.688+0800: #0: [DefNew: 69952K->8704K(78656K), 0.0591895 secs] 69952K->56788K(253440K), 0.0592437 secs] [Times: user=0.05 sys=0.02, real=0.06 secs] \n" +
                        "2021-12-07T11:18:11.756+0800: #1: [GC (Allocation Failure) 2021-12-07T11:18:11.756+0800: #1: [DefNew: 78656K->8703K(78656K), 0.0700624 secs] 126740K->114869K(253440K), 0.0701086 secs] [Times: user=0.05 sys=0.01, real=0.07 secs] \n" +
                        "2021-12-07T11:18:11.833+0800: #2: [GC (Allocation Failure) 2021-12-07T11:18:11.833+0800: #2: [DefNew: 78655K->8703K(78656K), 0.0837783 secs]2021-12-07T11:18:11.917+0800: #3: [Tenured: 176115K->174136K(176128K), 0.1988447 secs] 184821K->174136K(254784K), [Metaspace: 3244K->3244K(1056768K)], 0.2828418 secs] [Times: user=0.27 sys=0.02, real=0.28 secs] \n" +
                        "2021-12-07T11:18:12.140+0800: #4: [GC (Allocation Failure) 2021-12-07T11:18:12.140+0800: #4: [DefNew: 116224K->14463K(130688K), 0.1247689 secs] 290360K->290358K(420916K), 0.1248360 secs] [Times: user=0.10 sys=0.03, real=0.12 secs] \n" +
                        "2021-12-07T11:18:12.273+0800: #5: [GC (Allocation Failure) 2021-12-07T11:18:12.273+0800: #5: [DefNew: 102309K->14463K(130688K), 0.1181527 secs]2021-12-07T11:18:12.391+0800: #6: [Tenured: 362501K->362611K(362612K), 0.3681604 secs] 378203K->376965K(493300K), [Metaspace: 3244K->3244K(1056768K)], 0.4867024 secs] [Times: user=0.46 sys=0.03, real=0.49 secs] \n" +
                        "2021-12-07T11:18:12.809+0800: #7: [GC (Allocation Failure) 2021-12-07T11:18:12.809+0800: #7: [DefNew: 227109K->30207K(272000K), 0.3180977 secs] 589721K->581277K(876356K), 0.3181286 secs] [Times: user=0.27 sys=0.05, real=0.32 secs] \n" +
                        "2021-12-07T11:18:13.160+0800: #8: [GC (Allocation Failure) 2021-12-07T11:18:13.160+0800: #8: [DefNew: 271999K->30207K(272000K), 0.2782985 secs]2021-12-07T11:18:13.438+0800: #9: [Tenured: 785946K->756062K(786120K), 0.8169720 secs] 823069K->756062K(1058120K), [Metaspace: 3782K->3782K(1056768K)], 1.0959870 secs] [Times: user=1.03 sys=0.07, real=1.09 secs] \n" +
                        "2021-12-07T11:18:14.386+0800: #10: [GC (Allocation Failure) 2021-12-07T11:18:14.386+0800: #10: [DefNew: 504128K->62975K(567104K), 0.5169362 secs] 1260190K->1260189K(1827212K), 0.5169650 secs] [Times: user=0.40 sys=0.12, real=0.52 secs] ";

        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        SerialGCModel model = (SerialGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 8);
        Assert.assertEquals(model.getReferenceTimestamp(), 1638847091688.0, DELTA);

        GCEvent youngGC = model.getGcEvents().get(1);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getStartTime(), 68, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 70.1086, DELTA);
        GCMemoryItem youngGen = youngGC.getMemoryItem(YOUNG);
        Assert.assertEquals(youngGen.getPreUsed(), 78656 * 1024);
        Assert.assertEquals(youngGen.getPostUsed(), 8703 * 1024);
        Assert.assertEquals(youngGen.getPostCapacity(), 78656 * 1024);
        GCMemoryItem total = youngGC.getMemoryItem(HEAP);
        Assert.assertEquals(total.getPreUsed(), 126740 * 1024);
        Assert.assertEquals(total.getPostUsed(), 114869 * 1024);
        Assert.assertEquals(total.getPostCapacity(), 253440 * 1024);
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 70, DELTA);

        GCEvent fullGC = model.getGcEvents().get(6);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getStartTime(), 1472, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 1095.987, DELTA);
        youngGen = fullGC.getMemoryItem(YOUNG);
        Assert.assertEquals(youngGen.getPreUsed(), 271999 * 1024);
        Assert.assertEquals(youngGen.getPostUsed(), 0);
        Assert.assertEquals(youngGen.getPostCapacity(), 272000 * 1024);
        GCMemoryItem oldGen = fullGC.getMemoryItem(OLD);
        Assert.assertEquals(oldGen.getPreUsed(), 785946 * 1024);
        Assert.assertEquals(oldGen.getPostUsed(), 756062 * 1024);
        Assert.assertEquals(oldGen.getPostCapacity(), 786120 * 1024);
        GCMemoryItem metaspace = fullGC.getMemoryItem(METASPACE);
        Assert.assertEquals(metaspace.getPreUsed(), 3782 * 1024);
        Assert.assertEquals(metaspace.getPostUsed(), 3782 * 1024);
        Assert.assertEquals(metaspace.getPostCapacity(), 1056768 * 1024);
        total = fullGC.getMemoryItem(HEAP);
        Assert.assertEquals(total.getPreUsed(), 823069 * 1024);
        Assert.assertEquals(total.getPostUsed(), 756062 * 1024);
        Assert.assertEquals(total.getPostCapacity(), 1058120 * 1024);
        Assert.assertEquals(fullGC.getCpuTime().getSys(), 70, DELTA);
    }

    @Test
    public void testJDK8GenerationalGCInterleave() throws Exception {
        String log =
                "2022-08-02T10:26:05.043+0800: 61988.328: [GC (Allocation Failure) 2022-08-02T10:26:05.043+0800: 61988.328: [ParNew: 2621440K->2621440K(2883584K), 0.0000519 secs]2022-08-02T10:26:05.043+0800: 61988.328: [CMS: 1341593K->1329988K(2097152K), 2.0152293 secs] 3963033K->1329988K(4980736K), [Metaspace: 310050K->309844K(1343488K)], 2.0160411 secs] [Times: user=1.98 sys=0.05, real=2.01 secs] ";

        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        GCModel model = parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 1);
        GCEvent fullGC = model.getGcEvents().get(0);
        Assert.assertEquals(fullGC.getStartTime(), 61988328, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 2016.0411, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 2621440L * 1024, 0, 2883584L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 1341593L * 1024, 1329988L * 1024, 2097152L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 3963033L * 1024, 1329988L * 1024, 4980736L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 310050L * 1024, 309844L * 1024, 1343488L * 1024));
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 2010, DELTA);
    }


    @Test
    public void testJDK11GenerationalGCInterleave() throws Exception {
        String log =
                "[5.643s][info][gc,start     ] GC(3) Pause Young (Allocation Failure)\n" +
                        "[5.643s][info][gc,start     ] GC(4) Pause Full (Allocation Failure)\n" +
                        "[5.643s][info][gc,phases,start] GC(4) Phase 1: Mark live objects\n" +
                        "[5.691s][info][gc,phases      ] GC(4) Phase 1: Mark live objects 47.363ms\n" +
                        "[5.691s][info][gc,phases,start] GC(4) Phase 2: Compute new object addresses\n" +
                        "[5.715s][info][gc,phases      ] GC(4) Phase 2: Compute new object addresses 24.314ms\n" +
                        "[5.715s][info][gc,phases,start] GC(4) Phase 3: Adjust pointers\n" +
                        "[5.771s][info][gc,phases      ] GC(4) Phase 3: Adjust pointers 56.294ms\n" +
                        "[5.771s][info][gc,phases,start] GC(4) Phase 4: Move objects\n" +
                        "[5.789s][info][gc,phases      ] GC(4) Phase 4: Move objects 17.974ms\n" +
                        "[5.789s][info][gc             ] GC(4) Pause Full (Allocation Failure) 215M->132M(247M) 146.153ms\n" +
                        "[5.789s][info][gc,heap        ] GC(3) DefNew: 78655K->0K(78656K)\n" +
                        "[5.789s][info][gc,heap        ] GC(3) Tenured: 142112K->135957K(174784K)\n" +
                        "[5.789s][info][gc,metaspace   ] GC(3) Metaspace: 7462K->7462K(1056768K)\n" +
                        "[5.789s][info][gc             ] GC(3) Pause Young (Allocation Failure) 215M->132M(247M) 146.211ms\n" +
                        "[5.789s][info][gc,cpu         ] GC(3) User=0.15s Sys=0.00s Real=0.15s";

        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        SerialGCModel model = (SerialGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 1);
        GCEvent fullGC = model.getGcEvents().get(0);
        Assert.assertEquals(fullGC.getGcid(), 3);
        Assert.assertEquals(fullGC.getStartTime(), 5643, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 146.211, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 78655 * 1024, 0, 78656 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 142112 * 1024, 135957 * 1024, 174784 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 215 * 1024 * 1024, 132 * 1024 * 1024, 247 * 1024 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 7462 * 1024, 7462 * 1024, 1056768 * 1024));
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 24.314, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 17.974, DELTA);
        Assert.assertTrue(fullGC.isTrue(YOUNG_GC_BECOME_FULL_GC));
    }

    @Test
    public void TestIncompleteGCLog() throws Exception {
        String log =
                "[0.510s][info][gc,heap      ] GC(0) CMS: 0K->24072K(174784K)\n" +
                        "[0.510s][info][gc,metaspace ] GC(0) Metaspace: 6531K->6530K(1056768K)\n" +
                        "[0.510s][info][gc           ] GC(0) Pause Young (Allocation Failure) 68M->32M(247M) 31.208ms\n" +
                        "[0.510s][info][gc,cpu       ] GC(0) User=0.06s Sys=0.03s Real=0.03s\n" +
                        "[3.231s][info][gc,start     ] GC(1) Pause Initial Mark\n" +
                        "[3.235s][info][gc           ] GC(1) Pause Initial Mark 147M->147M(247M) 3.236ms\n" +
                        "[3.235s][info][gc,cpu       ] GC(1) User=0.01s Sys=0.02s Real=0.03s\n" +
                        "[3.235s][info][gc           ] GC(1) Concurrent Mark\n" +
                        "[3.235s][info][gc,task      ] GC(1) Using 2 workers of 2 for marking\n" +
                        "[3.257s][info][gc           ] GC(1) Concurrent Mark 22.229ms\n" +
                        "[3.257s][info][gc,cpu       ] GC(1) User=0.07s Sys=0.00s Real=0.03s\n" +
                        "[3.257s][info][gc           ] GC(1) Concurrent Preclean\n" +
                        "[3.257s][info][gc           ] GC(1) Concurrent Preclean 0.264ms\n" +
                        "[3.257s][info][gc,cpu       ] GC(1) User=0.00s Sys=0.00s Real=0.00s\n" +
                        "[3.257s][info][gc,start     ] GC(1) Pause Remark\n" +
                        "[3.259s][info][gc           ] GC(1) Pause Remark 149M->149M(247M) 1.991ms\n" +
                        "[3.259s][info][gc,cpu       ] GC(1) User=0.02s Sys=0.03s Real=0.01s\n" +
                        "[3.259s][info][gc           ] GC(1) Concurrent Sweep\n" +
                        "[3.279s][info][gc           ] GC(1) Concurrent Sweep 19.826ms\n" +
                        "[3.279s][info][gc,cpu       ] GC(1) User=0.03s Sys=0.00s Real=0.02s\n" +
                        "[3.279s][info][gc           ] GC(1) Concurrent Reset\n" +
                        "[3.280s][info][gc           ] GC(1) Concurrent Reset 0.386ms\n" +
                        "[3.280s][info][gc,cpu       ] GC(1) User=0.00s Sys=0.00s Real=0.00s\n" +
                        "[3.280s][info][gc,heap      ] GC(1) Old: 142662K->92308K(174784K)\n" +
                        "[8.970s][info][gc,start     ] GC(2) Pause Full (Allocation Failure)\n" +
                        "[8.970s][info][gc,phases,start] GC(2) Phase 1: Mark live objects\n" +
                        "[9.026s][info][gc,phases      ] GC(2) Phase 1: Mark live objects 55.761ms\n" +
                        "[9.026s][info][gc,phases,start] GC(2) Phase 2: Compute new object addresses\n" +
                        "[9.051s][info][gc,phases      ] GC(2) Phase 2: Compute new object addresses 24.761ms\n" +
                        "[9.051s][info][gc,phases,start] GC(2) Phase 3: Adjust pointers\n";

        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));

        CMSGCModel model = (CMSGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 2);
        Assert.assertEquals(model.getAllEvents().size(), 8);
    }

    @Test
    public void testJDK8ConcurrentPrintDateTimeStamp() throws Exception {
        String log = "2022-04-25T11:38:47.548+0800: 725.062: [GC pause (G1 Evacuation Pause) (young) (initial-mark) 725.062: [G1Ergonomics (CSet Construction) start choosing CSet, _pending_cards: 5369, predicted base time: 22.02 ms, remaining time: 177.98 ms, target pause time: 200.00 ms]\n" +
                " 725.062: [G1Ergonomics (CSet Construction) add young regions to CSet, eden: 10 regions, survivors: 1 regions, predicted young region time: 2.34 ms]\n" +
                " 725.062: [G1Ergonomics (CSet Construction) finish choosing CSet, eden: 10 regions, survivors: 1 regions, old: 0 regions, predicted pause time: 24.37 ms, target pause time: 200.00 ms]\n" +
                ", 0.0182684 secs]\n" +
                "   [Parallel Time: 17.4 ms, GC Workers: 4]\n" +
                "      [GC Worker Start (ms): Min: 725063.0, Avg: 725063.0, Max: 725063.0, Diff: 0.0]\n" +
                "      [Ext Root Scanning (ms): Min: 7.6, Avg: 7.9, Max: 8.4, Diff: 0.8, Sum: 31.6]\n" +
                "      [Update RS (ms): Min: 2.6, Avg: 2.7, Max: 2.9, Diff: 0.3, Sum: 10.8]\n" +
                "         [Processed Buffers: Min: 6, Avg: 6.8, Max: 7, Diff: 1, Sum: 27]\n" +
                "      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]\n" +
                "      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]\n" +
                "      [Object Copy (ms): Min: 5.4, Avg: 6.1, Max: 6.5, Diff: 1.1, Sum: 24.5]\n" +
                "      [Termination (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]\n" +
                "         [Termination Attempts: Min: 1, Avg: 4.8, Max: 8, Diff: 7, Sum: 19]\n" +
                "      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]\n" +
                "      [GC Worker Total (ms): Min: 16.8, Avg: 16.8, Max: 16.8, Diff: 0.0, Sum: 67.1]\n" +
                "      [GC Worker End (ms): Min: 725079.8, Avg: 725079.8, Max: 725079.8, Diff: 0.0]\n" +
                "   [Code Root Fixup: 0.0 ms]\n" +
                "   [Code Root Purge: 0.0 ms]\n" +
                "   [Clear CT: 0.1 ms]\n" +
                "   [Other: 0.7 ms]\n" +
                "      [Choose CSet: 0.0 ms]\n" +
                "      [Ref Proc: 0.1 ms]\n" +
                "      [Ref Enq: 0.0 ms]\n" +
                "      [Redirty Cards: 0.1 ms]\n" +
                "      [Humongous Register: 0.0 ms]\n" +
                "      [Humongous Reclaim: 0.0 ms]\n" +
                "      [Free CSet: 0.0 ms]\n" +
                "   [Eden: 320.0M(320.0M)->0.0B(320.0M) Survivors: 32768.0K->32768.0K Heap: 2223.9M(2560.0M)->1902.5M(2560.0M)]\n" +
                " [Times: user=0.07 sys=0.00, real=0.02 secs]\n" +
                /*
                 * This test mainly test the line below. The format here is:
                 * [DateStamp] [DateStamp] [TimeStamp] [TimeStamp] [Safepoint]
                 * [Concurrent cycle phase]
                 */
                "2022-04-25T11:38:47.567+0800: 2022-04-25T11:38:47.567+0800: 725.081: 725.081: Total time for which application threads were stopped: 0.0227079 seconds, Stopping threads took: 0.0000889 seconds\n" +
                "[GC concurrent-root-region-scan-start]\n" +
                "2022-04-25T11:38:47.581+0800: 725.095: Application time: 0.0138476 seconds\n" +
                "2022-04-25T11:38:47.585+0800: 725.099: Total time for which application threads were stopped: 0.0042001 seconds, Stopping threads took: 0.0000809 seconds\n" +
                "2022-04-25T11:38:47.613+0800: 725.127: [GC concurrent-root-region-scan-end, 0.0460720 secs]\n" +
                "2022-04-25T11:38:47.613+0800: 725.127: [GC concurrent-mark-start]\n" +
                "2022-04-25T11:38:51.924+0800: 729.438: [GC pause (G1 Evacuation Pause) (young) 729.438: [G1Ergonomics (CSet Construction) start choosing CSet, _pending_cards: 4375, predicted base time: 22.74 ms, remaining time: 177.26 ms, target pause time: 200.00 ms]\n" +
                " 729.438: [G1Ergonomics (CSet Construction) add young regions to CSet, eden: 10 regions, survivors: 1 regions, predicted young region time: 4.90 ms]\n" +
                " 729.438: [G1Ergonomics (CSet Construction) finish choosing CSet, eden: 10 regions, survivors: 1 regions, old: 0 regions, predicted pause time: 27.64 ms, target pause time: 200.00 ms]\n" +
                ", 0.0535660 secs]\n" +
                "   [Parallel Time: 52.5 ms, GC Workers: 4]\n" +
                "      [GC Worker Start (ms): Min: 729438.4, Avg: 729438.5, Max: 729438.5, Diff: 0.0]\n" +
                "      [Ext Root Scanning (ms): Min: 5.2, Avg: 5.9, Max: 6.9, Diff: 1.8, Sum: 23.7]\n" +
                "      [Update RS (ms): Min: 1.7, Avg: 2.5, Max: 3.3, Diff: 1.7, Sum: 10.0]\n" +
                "         [Processed Buffers: Min: 4, Avg: 6.0, Max: 7, Diff: 3, Sum: 24]\n" +
                "      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]\n" +
                "      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]\n" +
                "      [Object Copy (ms): Min: 5.6, Avg: 5.7, Max: 6.1, Diff: 0.5, Sum: 22.9]\n" +
                "      [Termination (ms): Min: 37.2, Avg: 37.5, Max: 38.2, Diff: 1.0, Sum: 149.9]\n" +
                "         [Termination Attempts: Min: 1, Avg: 10.8, Max: 17, Diff: 16, Sum: 43]\n" +
                "      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]\n" +
                "      [GC Worker Total (ms): Min: 51.4, Avg: 51.7, Max: 52.4, Diff: 0.9, Sum: 206.7]\n" +
                "      [GC Worker End (ms): Min: 729489.9, Avg: 729490.1, Max: 729490.8, Diff: 0.9]\n" +
                "   [Code Root Fixup: 0.0 ms]\n" +
                "   [Code Root Purge: 0.0 ms]\n" +
                "   [Clear CT: 0.1 ms]\n" +
                "   [Other: 1.0 ms]\n" +
                "      [Choose CSet: 0.0 ms]\n" +
                "      [Ref Proc: 0.4 ms]\n" +
                "      [Ref Enq: 0.0 ms]\n" +
                "      [Redirty Cards: 0.0 ms]\n" +
                "      [Humongous Register: 0.0 ms]\n" +
                "      [Humongous Reclaim: 0.0 ms]\n" +
                "      [Free CSet: 0.0 ms]\n" +
                "   [Eden: 320.0M(320.0M)->0.0B(320.0M) Survivors: 32768.0K->32768.0K Heap: 2230.5M(2560.0M)->1906.2M(2560.0M)]\n" +
                " [Times: user=0.06 sys=0.01, real=0.05 secs]\n" +
                "2022-04-25T11:38:51.978+0800: 729.492: Total time for which application threads were stopped: 0.0578224 seconds, Stopping threads took: 0.0000709 seconds\n" +
                "2022-04-25T11:38:52.409+0800: 729.923: Application time: 0.4310531 seconds\n" +
                "2022-04-25T11:38:52.944+0800: 730.458: [GC concurrent-mark-end, 5.3312732 secs]\n" +
                "2022-04-25T11:38:52.944+0800: 730.458: Application time: 0.1087156 seconds\n" +
                "2022-04-25T11:38:52.949+0800: 730.463: [GC remark 2022-04-25T11:38:52.949+0800: 730.463: [Finalize Marking, 0.0014784 secs] 2022-04-25T11:38:52.950+0800: 730.464: [GC ref-proc, 0.0007278 secs] 2022-04-25T11:38:52.951+0800: 730.465: [Unloading, 0.1281692 secs], 0.1350560 secs]\n" +
                " [Times: user=0.21 sys=0.01, real=0.13 secs]\n" +
                "2022-04-25T11:38:53.084+0800: 730.598: Total time for which application threads were stopped: 0.1396855 seconds, Stopping threads took: 0.0000545 seconds\n" +
                "2022-04-25T11:38:53.084+0800: 730.598: Application time: 0.0000928 seconds\n" +
                "2022-04-25T11:38:53.089+0800: 730.603: [GC cleanup 1984M->1984M(2560M), 0.0016114 secs]\n" +
                " [Times: user=0.01 sys=0.00, real=0.01 secs]\n";
        PreUnifiedG1GCLogParser parser = (PreUnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));
        G1GCModel model = (G1GCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertEquals(model.getGcEvents().size(), 3);
        Assert.assertEquals(model.getGcEvents().get(0).getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(model.getGcEvents().get(1).getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assert.assertEquals(model.getGcEvents().get(1).getPhases().get(0).getEventType(), GCEventType.G1_CONCURRENT_SCAN_ROOT_REGIONS);
        Assert.assertEquals(model.getGcEvents().get(1).getPhases().get(0).getStartTime(), 725081, DELTA);
        Assert.assertEquals(model.getGcEvents().get(2).getEventType(), GCEventType.YOUNG_GC);
    }

    @Test
    public void TestJDK8CMSPromotionFailed() throws Exception {
        String log = "2021-09-24T22:54:19.430+0800: 23501.549: [GC (Allocation Failure) 2021-09-24T22:54:19.430+0800: 23501.550: [ParNew (promotion failed): 7689600K->7689600K(7689600K), 5.2751800 secs]2021-09-24T22:54:24.705+0800: 23506.825: [CMS: 9258265K->5393434K(12582912K), 14.5693099 secs] 16878013K->5393434K(20272512K), [Metaspace: 208055K->203568K(1253376K)], 19.8476364 secs] [Times: user=19.95 sys=0.05, real=19.85 secs]";
        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        CMSGCModel model = (CMSGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assert.assertEquals(model.getGcEvents().size(), 1);
        GCEvent fullGC = model.getGcEvents().get(0);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 7689600L * 1024, 0, 7689600L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 9258265L * 1024, 5393434L * 1024, 12582912L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 16878013L * 1024, 5393434L * 1024, 20272512L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 208055 * 1024, 203568 * 1024, 1253376 * 1024));
        Assert.assertEquals(fullGC.getCause(), PROMOTION_FAILED);
        Assert.assertTrue(fullGC.isTrue(YOUNG_GC_BECOME_FULL_GC));

    }

    @Test
    public void TestJDK8CMSScavengeBeforeRemark() throws Exception {
        String log = "2022-10-14T14:28:44.426+0800: 165.382: [GC (CMS Initial Mark) [1 CMS-initial-mark: 1308232K(2097152K)] 2715777K(4019584K), 0.2266431 secs] [Times: user=0.85 sys=0.03, real=0.23 secs] \n" +
                "2022-10-14T14:28:44.653+0800: 165.609: [CMS-concurrent-mark-start]\n" +
                "2022-10-14T14:28:46.815+0800: 167.771: [CMS-concurrent-mark: 2.082/2.161 secs] [Times: user=4.22 sys=0.58, real=2.16 secs] \n" +
                "2022-10-14T14:28:46.815+0800: 167.771: [CMS-concurrent-preclean-start]\n" +
                "2022-10-14T14:28:46.854+0800: 167.810: [CMS-concurrent-preclean: 0.038/0.039 secs] [Times: user=0.12 sys=0.01, real=0.04 secs] \n" +
                "2022-10-14T14:28:46.855+0800: 167.811: [CMS-concurrent-abortable-preclean-start]\n" +
                "2022-10-14T14:28:47.937+0800: 168.893: [GC (Allocation Failure) 2022-10-14T14:28:47.937+0800: 168.893: [ParNew: 1922431K->174720K(1922432K), 0.2928759 secs] 3230664K->1560308K(4019584K), 0.2931600 secs] [Times: user=0.83 sys=0.06, real=0.29 secs] \n" +
                "2022-10-14T14:28:50.764+0800: 171.720: [CMS-concurrent-abortable-preclean: 3.498/3.909 secs] [Times: user=10.64 sys=1.14, real=3.91 secs] \n" +
                "2022-10-14T14:28:50.765+0800: 171.721: [GC (CMS Final Remark) [YG occupancy: 1056998 K (1922432 K)]2022-10-14T14:28:50.765+0800: 171.721: [GC (CMS Final Remark) 2022-10-14T14:28:50.765+0800: 171.721: [ParNew: 1056998K->151245K(1922432K), 0.1588173 secs] 2442587K->1615175K(4019584K), 0.1590607 secs] [Times: user=0.49 sys=0.05, real=0.16 secs] \n" +
                "2022-10-14T14:28:50.924+0800: 171.880: [Rescan (parallel) , 0.0482726 secs]2022-10-14T14:28:50.973+0800: 171.929: [weak refs processing, 0.0000506 secs]2022-10-14T14:28:50.973+0800: 171.929: [class unloading, 0.0809186 secs]2022-10-14T14:28:51.054+0800: 172.010: [scrub symbol table, 0.0649216 secs]2022-10-14T14:28:51.118+0800: 172.075: [scrub string table, 0.0045311 secs][1 CMS-remark: 1463930K(2097152K)] 1615175K(4019584K), 0.3629243 secs] [Times: user=0.83 sys=0.06, real=0.36 secs] \n" +
                "2022-10-14T14:28:51.129+0800: 172.085: [CMS-concurrent-sweep-start]\n" +
                "2022-10-14T14:28:51.881+0800: 172.837: [CMS-concurrent-sweep: 0.727/0.752 secs] [Times: user=1.41 sys=0.20, real=0.75 secs] \n" +
                "2022-10-14T14:28:51.881+0800: 172.837: [CMS-concurrent-reset-start]\n" +
                "2022-10-14T14:28:51.895+0800: 172.851: [CMS-concurrent-reset: 0.014/0.014 secs] [Times: user=0.03 sys=0.01, real=0.02 secs] ";
        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        CMSGCModel model = (CMSGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assert.assertEquals(model.getGcEvents().size(), 3);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 830, DELTA);

        GCEvent youngGC = model.getGcEvents().get(1);
        Assert.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 1922431L * 1024L, 174720L * 1024L, 1922432L * 1024L));

        youngGC = model.getGcEvents().get(2);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 1056998L * 1024L, 151245L * 1024L, 1922432L * 1024L));
        Assert.assertEquals(youngGC.getCpuTime().getUser(), 490, DELTA);
        Assert.assertEquals(youngGC.getCause(), CMS_FINAL_REMARK);

        PauseStatistics pause = model.getPauseStatistics(new TimeRange(0, 99999999));
        Assert.assertEquals(pause.getPauseAvg(), (226.6431 + 362.9243 + 293.1600) / 3.0, DELTA);
    }

    @Test
    public void TestJDK11CMSScavengeBeforeRemark() throws Exception {
        String log = "[0.600s][info][gc,start     ] GC(1) Pause Young (Allocation Failure)\n" +
                "[0.600s][info][gc,task      ] GC(1) Using 2 workers of 8 for evacuation\n" +
                "[0.632s][info][gc,heap      ] GC(1) ParNew: 46079K->5120K(46080K)\n" +
                "[0.632s][info][gc,heap      ] GC(1) CMS: 14224K->48865K(51200K)\n" +
                "[0.632s][info][gc,metaspace ] GC(1) Metaspace: 6590K->6590K(1056768K)\n" +
                "[0.632s][info][gc           ] GC(1) Pause Young (Allocation Failure) 58M->52M(95M) 32.662ms\n" +
                "[0.632s][info][gc,cpu       ] GC(1) User=0.05s Sys=0.01s Real=0.03s\n" +
                "[0.632s][info][gc,start     ] GC(2) Pause Initial Mark\n" +
                "[0.635s][info][gc           ] GC(2) Pause Initial Mark 53M->53M(95M) 2.784ms\n" +
                "[0.635s][info][gc,cpu       ] GC(2) User=0.03s Sys=0.00s Real=0.00s\n" +
                "[0.635s][info][gc           ] GC(2) Concurrent Mark\n" +
                "[0.635s][info][gc,task      ] GC(2) Using 2 workers of 2 for marking\n" +
                "[0.642s][info][gc           ] GC(2) Concurrent Mark 6.796ms\n" +
                "[0.642s][info][gc,cpu       ] GC(2) User=0.02s Sys=0.00s Real=0.01s\n" +
                "[0.642s][info][gc           ] GC(2) Concurrent Preclean\n" +
                "[0.642s][info][gc           ] GC(2) Concurrent Preclean 0.110ms\n" +
                "[0.642s][info][gc,cpu       ] GC(2) User=0.02s Sys=0.00s Real=0.00s\n" +
                "[0.642s][info][gc,start     ] GC(2) Pause Remark\n" +
                "[0.642s][info][gc,start     ] GC(3) Pause Young (CMS Final Remark)\n" +
                "[0.642s][info][gc,task      ] GC(3) Using 2 workers of 8 for evacuation\n" +
                "[0.642s][info][gc,heap      ] GC(3) ParNew: 5923K->5921K(46080K)\n" +
                "[0.642s][info][gc,heap      ] GC(3) CMS: 48865K->48865K(51200K)\n" +
                "[0.642s][info][gc,metaspace ] GC(3) Metaspace: 6590K->6590K(1056768K)\n" +
                "[0.642s][info][gc           ] GC(3) Pause Young (CMS Final Remark) 53M->53M(95M) 0.040ms\n" +
                "[0.642s][info][gc,cpu       ] GC(3) User=0.07s Sys=0.00s Real=0.00s\n" +
                "[0.646s][info][gc           ] GC(2) Pause Remark 53M->53M(95M) 3.259ms\n" +
                "[0.646s][info][gc,cpu       ] GC(2) User=0.09s Sys=0.00s Real=0.00s\n" +
                "[0.646s][info][gc           ] GC(2) Concurrent Sweep\n" +
                "[0.654s][info][gc           ] GC(2) Concurrent Sweep 8.299ms\n" +
                "[0.654s][info][gc,cpu       ] GC(2) User=0.01s Sys=0.00s Real=0.01s\n" +
                "[0.654s][info][gc           ] GC(2) Concurrent Reset\n" +
                "[0.654s][info][gc           ] GC(2) Concurrent Reset 0.046ms\n" +
                "[0.654s][info][gc,cpu       ] GC(2) User=0.04s Sys=0.00s Real=0.00s\n" +
                "[0.654s][info][gc,heap      ] GC(2) Old: 48865K->34645K(51200K)";
        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        CMSGCModel model = (CMSGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assert.assertEquals(model.getGcEvents().size(), 3);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 90, DELTA);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 46079 * 1024L, 5120 * 1024L, 46080 * 1024L));

        youngGC = model.getGcEvents().get(2);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 5923 * 1024L, 5921 * 1024L, 46080 * 1024L));
        Assert.assertEquals(youngGC.getCpuTime().getUser(), 70, DELTA);
        Assert.assertEquals(youngGC.getCause(), CMS_FINAL_REMARK);

        PauseStatistics pause = model.getPauseStatistics(new TimeRange(0, 99999999));
        Assert.assertEquals(pause.getPauseAvg(), (32.662 + 3.259 + 2.784) / 3.0, DELTA);
    }

    @Test
    public void TestJDK17SerialGCParser() throws Exception {
        String log = "[0.008s][info][gc] Using Serial\n" +
                "[0.008s][info][gc,init] Version: 17.0.1+12-39 (release)\n" +
                "[0.008s][info][gc,init] CPUs: 8 total, 8 available\n" +
                "[0.008s][info][gc,init] Memory: 16384M\n" +
                "[0.008s][info][gc,init] Large Page Support: Disabled\n" +
                "[0.008s][info][gc,init] NUMA Support: Disabled\n" +
                "[0.008s][info][gc,init] Compressed Oops: Enabled (Zero based)\n" +
                "[0.008s][info][gc,init] Heap Min Capacity: 100M\n" +
                "[0.008s][info][gc,init] Heap Initial Capacity: 100M\n" +
                "[0.008s][info][gc,init] Heap Max Capacity: 100M\n" +
                "[0.008s][info][gc,init] Pre-touch: Disabled\n" +
                "[0.008s][info][gc,metaspace] CDS archive(s) mapped at: [0x0000000800000000-0x0000000800bd4000-0x0000000800bd4000), size 12402688, SharedBaseAddress: 0x0000000800000000, ArchiveRelocationMode: 0.\n" +
                "[0.008s][info][gc,metaspace] Compressed class space mapped at: 0x0000000800c00000-0x0000000840c00000, reserved size: 1073741824\n" +
                "[0.008s][info][gc,metaspace] Narrow klass base: 0x0000000800000000, Narrow klass shift: 0, Narrow klass range: 0x100000000\n" +
                "[0.173s][info][gc,start    ] GC(0) Pause Young (Allocation Failure)\n" +
                "[0.194s][info][gc,heap     ] GC(0) DefNew: 40960K(46080K)->5120K(46080K) Eden: 40960K(40960K)->0K(40960K) From: 0K(5120K)->5120K(5120K)\n" +
                "[0.194s][info][gc,heap     ] GC(0) Tenured: 0K(51200K)->14524K(51200K)\n" +
                "[0.194s][info][gc,metaspace] GC(0) Metaspace: 137K(384K)->138K(384K) NonClass: 133K(256K)->134K(256K) Class: 4K(128K)->4K(128K)\n" +
                "[0.194s][info][gc          ] GC(0) Pause Young (Allocation Failure) 40M->19M(95M) 21.766ms\n" +
                "[0.194s][info][gc,cpu      ] GC(0) User=0.01s Sys=0.00s Real=0.02s\n" +
                "[2.616s][info][gc,start       ] GC(1) Pause Full (Allocation Failure)\n" +
                "[2.616s][info][gc,phases,start] GC(1) Phase 1: Mark live objects\n" +
                "[2.665s][info][gc,phases      ] GC(1) Phase 1: Mark live objects 49.316ms\n" +
                "[2.665s][info][gc,phases,start] GC(1) Phase 2: Compute new object addresses\n" +
                "[2.677s][info][gc,phases      ] GC(1) Phase 2: Compute new object addresses 12.103ms\n" +
                "[2.677s][info][gc,phases,start] GC(1) Phase 3: Adjust pointers\n" +
                "[2.698s][info][gc,phases      ] GC(1) Phase 3: Adjust pointers 20.186ms\n" +
                "[2.698s][info][gc,phases,start] GC(1) Phase 4: Move objects\n" +
                "[2.708s][info][gc,phases      ] GC(1) Phase 4: Move objects 10.313ms\n" +
                "[2.708s][info][gc,heap        ] GC(1) DefNew: 46079K(46080K)->36798K(46080K) Eden: 40960K(40960K)->36798K(40960K) From: 5119K(5120K)->0K(5120K)\n" +
                "[2.708s][info][gc,heap        ] GC(1) Tenured: 51199K(51200K)->51199K(51200K)\n" +
                "[2.708s][info][gc,metaspace   ] GC(1) Metaspace: 137K(384K)->137K(384K) NonClass: 133K(256K)->133K(256K) Class: 4K(128K)->4K(128K)\n" +
                "[2.708s][info][gc             ] GC(1) Pause Full (Allocation Failure) 94M->85M(95M) 92.137ms\n" +
                "[2.708s][info][gc,cpu         ] GC(1) User=0.09s Sys=0.00s Real=0.09s";
        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        SerialGCModel model = (SerialGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 173, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 21.766, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 40960 * 1024, 46080 * 1024, 5120 * 1024, 46080 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 40960 * 1024, 40960 * 1024, 0 * 1024, 40960 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(SURVIVOR), new GCMemoryItem(SURVIVOR, 0 * 1024, 5120 * 1024, 5120 * 1024, 5120 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, 51200 * 1024, 14524 * 1024, 51200 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 137 * 1024, 384 * 1024, 138 * 1024, 384 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(NONCLASS), new GCMemoryItem(NONCLASS, 133 * 1024, 256 * 1024, 134 * 1024, 256 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(CLASS), new GCMemoryItem(CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 40 * 1024 * 1024, 95 * 1024 * 1024, 19 * 1024 * 1024, 95 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 20, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assert.assertEquals(fullGC.getGcid(), 1);
        Assert.assertEquals(fullGC.getStartTime(), 2616, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 92.137, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 46079 * 1024, 46080 * 1024, 36798 * 1024, 46080 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 40960 * 1024, 40960 * 1024, 36798 * 1024, 40960 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(SURVIVOR), new GCMemoryItem(SURVIVOR, 5119 * 1024, 5120 * 1024, 0 * 1024, 5120 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 51199 * 1024, 51200 * 1024, 51199 * 1024, 51200 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 137 * 1024, 384 * 1024, 137 * 1024, 384 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(NONCLASS), new GCMemoryItem(NONCLASS, 133 * 1024, 256 * 1024, 133 * 1024, 256 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(CLASS), new GCMemoryItem(CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 94 * 1024 * 1024, 95 * 1024 * 1024, 85 * 1024 * 1024, 95 * 1024 * 1024));
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 12.103, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 10.313, DELTA);
    }

    @Test
    public void TestJDK17ParallelGCParser() throws Exception {
        String log = "[0.017s][info][gc] Using Parallel\n" +
                "[0.018s][info][gc,init] Version: 17.0.1+12-39 (release)\n" +
                "[0.018s][info][gc,init] CPUs: 8 total, 8 available\n" +
                "[0.018s][info][gc,init] Memory: 16384M\n" +
                "[0.018s][info][gc,init] Large Page Support: Disabled\n" +
                "[0.018s][info][gc,init] NUMA Support: Disabled\n" +
                "[0.018s][info][gc,init] Compressed Oops: Enabled (Zero based)\n" +
                "[0.018s][info][gc,init] Alignments: Space 512K, Generation 512K, Heap 2M\n" +
                "[0.018s][info][gc,init] Heap Min Capacity: 100M\n" +
                "[0.018s][info][gc,init] Heap Initial Capacity: 100M\n" +
                "[0.018s][info][gc,init] Heap Max Capacity: 100M\n" +
                "[0.018s][info][gc,init] Pre-touch: Disabled\n" +
                "[0.018s][info][gc,init] Parallel Workers: 8\n" +
                "[0.019s][info][gc,metaspace] CDS archive(s) mapped at: [0x0000000800000000-0x0000000800bd4000-0x0000000800bd4000), size 12402688, SharedBaseAddress: 0x0000000800000000, ArchiveRelocationMode: 0.\n" +
                "[0.019s][info][gc,metaspace] Compressed class space mapped at: 0x0000000800c00000-0x0000000840c00000, reserved size: 1073741824\n" +
                "[0.019s][info][gc,metaspace] Narrow klass base: 0x0000000800000000, Narrow klass shift: 0, Narrow klass range: 0x100000000\n" +
                "[0.222s][info][gc,start    ] GC(0) Pause Young (Allocation Failure)\n" +
                "[0.232s][info][gc,heap     ] GC(0) PSYoungGen: 38912K(45056K)->6137K(45056K) Eden: 38912K(38912K)->0K(38912K) From: 0K(6144K)->6137K(6144K)\n" +
                "[0.232s][info][gc,heap     ] GC(0) ParOldGen: 0K(51200K)->13208K(51200K)\n" +
                "[0.232s][info][gc,metaspace] GC(0) Metaspace: 135K(384K)->135K(384K) NonClass: 131K(256K)->131K(256K) Class: 4K(128K)->4K(128K)\n" +
                "[0.232s][info][gc          ] GC(0) Pause Young (Allocation Failure) 38M->18M(94M) 10.085ms\n" +
                "[0.232s][info][gc,cpu      ] GC(0) User=0.02s Sys=0.01s Real=0.01s\n" +
                "[0.547s][info][gc,start    ] GC(1) Pause Full (Ergonomics)\n" +
                "[0.548s][info][gc,phases,start] GC(1) Marking Phase\n" +
                "[0.561s][info][gc,phases      ] GC(1) Marking Phase 13.555ms\n" +
                "[0.561s][info][gc,phases,start] GC(1) Summary Phase\n" +
                "[0.561s][info][gc,phases      ] GC(1) Summary Phase 0.006ms\n" +
                "[0.561s][info][gc,phases,start] GC(1) Adjust Roots\n" +
                "[0.561s][info][gc,phases      ] GC(1) Adjust Roots 0.238ms\n" +
                "[0.561s][info][gc,phases,start] GC(1) Compaction Phase\n" +
                "[0.568s][info][gc,phases      ] GC(1) Compaction Phase 6.917ms\n" +
                "[0.568s][info][gc,phases,start] GC(1) Post Compact\n" +
                "[0.568s][info][gc,phases      ] GC(1) Post Compact 0.222ms\n" +
                "[0.569s][info][gc,heap        ] GC(1) PSYoungGen: 6128K(45056K)->0K(45056K) Eden: 0K(38912K)->0K(38912K) From: 6128K(6144K)->0K(6144K)\n" +
                "[0.569s][info][gc,heap        ] GC(1) ParOldGen: 46504K(51200K)->38169K(51200K)\n" +
                "[0.569s][info][gc,metaspace   ] GC(1) Metaspace: 135K(384K)->135K(384K) NonClass: 131K(256K)->131K(256K) Class: 4K(128K)->4K(128K)\n" +
                "[0.569s][info][gc             ] GC(1) Pause Full (Ergonomics) 51M->37M(94M) 21.046ms\n" +
                "[0.569s][info][gc,cpu         ] GC(1) User=0.04s Sys=0.00s Real=0.02s";
        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        ParallelGCModel model = (ParallelGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 222, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 10.085, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 38912 * 1024, 45056 * 1024, 6137 * 1024, 45056 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 38912 * 1024, 38912 * 1024, 0 * 1024, 38912 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(SURVIVOR), new GCMemoryItem(SURVIVOR, 0 * 1024, 6144 * 1024, 6137 * 1024, 6144 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, 51200 * 1024, 13208 * 1024, 51200 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 135 * 1024, 384 * 1024, 135 * 1024, 384 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(NONCLASS), new GCMemoryItem(NONCLASS, 131 * 1024, 256 * 1024, 131 * 1024, 256 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(CLASS), new GCMemoryItem(CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 38 * 1024 * 1024, (45056 + 51200) * 1024, 18 * 1024 * 1024, 94 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 10, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assert.assertEquals(fullGC.getGcid(), 1);
        Assert.assertEquals(fullGC.getStartTime(), 547, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 21.046, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), ERGONOMICS);
        Assert.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 6128 * 1024, 45056 * 1024, 0 * 1024, 45056 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 0 * 1024, 38912 * 1024, 0 * 1024, 38912 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(SURVIVOR), new GCMemoryItem(SURVIVOR, 6128 * 1024, 6144 * 1024, 0 * 1024, 6144 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 46504 * 1024, 51200 * 1024, 38169 * 1024, 51200 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 135 * 1024, 384 * 1024, 135 * 1024, 384 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(NONCLASS), new GCMemoryItem(NONCLASS, 131 * 1024, 256 * 1024, 131 * 1024, 256 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(CLASS), new GCMemoryItem(CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 51 * 1024 * 1024, (45056 + 51200) * 1024, 37 * 1024 * 1024, 94 * 1024 * 1024));
        Assert.assertEquals(fullGC.getPhases().size(), 5);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_SUMMARY).getDuration(), 0.006, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_COMPACTION).getDuration(), 6.917, DELTA);
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 20, DELTA);
    }

    @Test
    public void testJDK17ZGCParser() throws Exception {
        String log =
                "[0.105s][info][gc,init] Initializing The Z Garbage Collector\n" +
                        "[0.105s][info][gc,init] Version: 17.0.1+12-39 (release)\n" +
                        "[0.105s][info][gc,init] NUMA Support: Disabled\n" +
                        "[0.105s][info][gc,init] CPUs: 8 total, 8 available\n" +
                        "[0.106s][info][gc,init] Memory: 16384M\n" +
                        "[0.106s][info][gc,init] Large Page Support: Disabled\n" +
                        "[0.106s][info][gc,init] GC Workers: 2 (dynamic)\n" +
                        "[0.107s][info][gc,init] Address Space Type: Contiguous/Unrestricted/Complete\n" +
                        "[0.107s][info][gc,init] Address Space Size: 16000M x 3 = 48000M\n" +
                        "[0.107s][info][gc,init] Min Capacity: 1000M\n" +
                        "[0.107s][info][gc,init] Initial Capacity: 1000M\n" +
                        "[0.107s][info][gc,init] Max Capacity: 1000M\n" +
                        "[0.107s][info][gc,init] Medium Page Size: 16M\n" +
                        "[0.107s][info][gc,init] Pre-touch: Disabled\n" +
                        "[0.107s][info][gc,init] Uncommit: Implicitly Disabled (-Xms equals -Xmx)\n" +
                        "[0.109s][info][gc,init] Runtime Workers: 5\n" +
                        "[0.111s][info][gc     ] Using The Z Garbage Collector\n" +
                        "[0.114s][info][gc,metaspace] CDS archive(s) mapped at: [0x0000000800000000-0x0000000800bac000-0x0000000800bac000), size 12238848, SharedBaseAddress: 0x0000000800000000, ArchiveRelocationMode: 0.\n" +
                        "[0.114s][info][gc,metaspace] Compressed class space mapped at: 0x0000000800c00000-0x0000000840c00000, reserved size: 1073741824\n" +
                        "[0.114s][info][gc,metaspace] Narrow klass base: 0x0000000800000000, Narrow klass shift: 0, Narrow klass range: 0x100000000\n" +
                        "[0.918s][info][gc,start    ] GC(0) Garbage Collection (Warmup)\n" +
                        "[0.918s][info][gc,task     ] GC(0) Using 2 workers\n" +
                        "[0.918s][info][gc,phases   ] GC(0) Pause Mark Start 0.007ms\n" +
                        "[0.939s][info][gc,phases   ] GC(0) Concurrent Mark 20.975ms\n" +
                        "[0.939s][info][gc,phases   ] GC(0) Pause Mark End 0.031ms\n" +
                        "[0.939s][info][gc,phases   ] GC(0) Concurrent Mark Free 0.001ms\n" +
                        "[0.940s][info][gc,phases   ] GC(0) Concurrent Process Non-Strong References 0.238ms\n" +
                        "[0.940s][info][gc,phases   ] GC(0) Concurrent Reset Relocation Set 0.001ms\n" +
                        "[0.948s][info][gc,phases   ] GC(0) Concurrent Select Relocation Set 8.843ms\n" +
                        "[0.949s][info][gc,phases   ] GC(0) Pause Relocate Start 0.008ms\n" +
                        "[0.949s][info][gc,phases   ] GC(0) Concurrent Relocate 0.811ms\n" +
                        "[0.950s][info][gc,load     ] GC(0) Load: 3.83/4.68/5.15\n" +
                        "[0.950s][info][gc,mmu      ] GC(0) MMU: 2ms/98.4%, 5ms/99.4%, 10ms/99.6%, 20ms/99.8%, 50ms/99.9%, 100ms/100.0%\n" +
                        "[0.950s][info][gc,marking  ] GC(0) Mark: 2 stripe(s), 2 proactive flush(es), 1 terminate flush(es), 0 completion(s), 0 continuation(s)\n" +
                        "[0.950s][info][gc,marking  ] GC(0) Mark Stack Usage: 32M\n" +
                        "[0.950s][info][gc,nmethod  ] GC(0) NMethods: 91 registered, 0 unregistered\n" +
                        "[0.950s][info][gc,metaspace] GC(0) Metaspace: 0M used, 0M committed, 1032M reserved\n" +
                        "[0.950s][info][gc,ref      ] GC(0) Soft: 5 encountered, 0 discovered, 0 enqueued\n" +
                        "[0.950s][info][gc,ref      ] GC(0) Weak: 5 encountered, 0 discovered, 0 enqueued\n" +
                        "[0.950s][info][gc,ref      ] GC(0) Final: 0 encountered, 0 discovered, 0 enqueued\n" +
                        "[0.950s][info][gc,ref      ] GC(0) Phantom: 1 encountered, 0 discovered, 0 enqueued\n" +
                        "[0.950s][info][gc,reloc    ] GC(0) Small Pages: 44 / 88M, Empty: 10M, Relocated: 1M, In-Place: 0\n" +
                        "[0.950s][info][gc,reloc    ] GC(0) Medium Pages: 1 / 16M, Empty: 0M, Relocated: 0M, In-Place: 0\n" +
                        "[0.950s][info][gc,reloc    ] GC(0) Large Pages: 0 / 0M, Empty: 0M, Relocated: 0M, In-Place: 0\n" +
                        "[0.950s][info][gc,reloc    ] GC(0) Forwarding Usage: 0M\n" +
                        "[0.950s][info][gc,heap     ] GC(0) Min Capacity: 1000M(100%)\n" +
                        "[0.950s][info][gc,heap     ] GC(0) Max Capacity: 1000M(100%)\n" +
                        "[0.950s][info][gc,heap     ] GC(0) Soft Max Capacity: 1000M(100%)\n" +
                        "[0.950s][info][gc,heap     ] GC(0)                Mark Start          Mark End        Relocate Start      Relocate End           High               Low\n" +
                        "[0.950s][info][gc,heap     ] GC(0)  Capacity:     1000M (100%)       1000M (100%)       1000M (100%)       1000M (100%)       1000M (100%)       1000M (100%)\n" +
                        "[0.950s][info][gc,heap     ] GC(0)      Free:      896M (90%)         892M (89%)         902M (90%)         912M (91%)         912M (91%)         892M (89%)\n" +
                        "[0.950s][info][gc,heap     ] GC(0)      Used:      104M (10%)         108M (11%)          98M (10%)          88M (9%)          108M (11%)          88M (9%)\n" +
                        "[0.950s][info][gc,heap     ] GC(0)      Live:         -                65M (7%)           65M (7%)           65M (7%)             -                  -\n" +
                        "[0.950s][info][gc,heap     ] GC(0) Allocated:         -                 4M (0%)            4M (0%)            3M (0%)             -                  -\n" +
                        "[0.950s][info][gc,heap     ] GC(0)   Garbage:         -                38M (4%)           28M (3%)           18M (2%)             -                  -\n" +
                        "[0.950s][info][gc,heap     ] GC(0) Reclaimed:         -                  -                10M (1%)           19M (2%)             -                  -\n" +
                        "[0.950s][info][gc          ] GC(0) Garbage Collection (Warmup) 104M(10%)->88M(9%)\n" +
                        "[10.417s][info][gc,stats    ] === Garbage Collection Statistics =======================================================================================================================\n" +
                        "[10.417s][info][gc,stats    ]                                                              Last 10s              Last 10m              Last 10h                Total\n" +
                        "[10.417s][info][gc,stats    ]                                                              Avg / Max             Avg / Max             Avg / Max             Avg / Max\n" +
                        "[10.417s][info][gc,stats    ]   Collector: Garbage Collection Cycle                     52.097 / 71.589       52.097 / 71.589       52.097 / 71.589       52.097 / 71.589      ms\n" +
                        "[10.417s][info][gc,stats    ]  Contention: Mark Segment Reset Contention                     0 / 1                 0 / 1                 0 / 1                 0 / 1           ops/s\n" +
                        "[10.417s][info][gc,stats    ]  Contention: Mark SeqNum Reset Contention                      0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[10.417s][info][gc,stats    ]    Critical: Allocation Stall                                  0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[10.417s][info][gc,stats    ]    Critical: Allocation Stall                              0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[10.418s][info][gc,stats    ]    Critical: GC Locker Stall                                   0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[10.418s][info][gc,stats    ]    Critical: GC Locker Stall                               0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[10.418s][info][gc,stats    ]    Critical: Relocation Stall                                  0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[10.418s][info][gc,stats    ]    Critical: Relocation Stall                              0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[10.418s][info][gc,stats    ]      Memory: Allocation Rate                                  48 / 162              48 / 162              48 / 162              48 / 162         MB/s\n" +
                        "[10.418s][info][gc,stats    ]      Memory: Out Of Memory                                     0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[10.418s][info][gc,stats    ]      Memory: Page Cache Flush                                  0 / 0                 0 / 0                 0 / 0                 0 / 0           MB/s\n" +
                        "[10.418s][info][gc,stats    ]      Memory: Page Cache Hit L1                                 4 / 15                4 / 15                4 / 15                4 / 15          ops/s\n" +
                        "[10.418s][info][gc,stats    ]      Memory: Page Cache Hit L2                                 0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[10.418s][info][gc,stats    ]      Memory: Page Cache Hit L3                                18 / 59               18 / 59               18 / 59               18 / 59          ops/s\n" +
                        "[10.418s][info][gc,stats    ]      Memory: Page Cache Miss                                   0 / 1                 0 / 1                 0 / 1                 0 / 1           ops/s\n" +
                        "[10.418s][info][gc,stats    ]      Memory: Uncommit                                          0 / 0                 0 / 0                 0 / 0                 0 / 0           MB/s\n" +
                        "[10.418s][info][gc,stats    ]      Memory: Undo Object Allocation Failed                     0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[10.418s][info][gc,stats    ]      Memory: Undo Object Allocation Succeeded                  7 / 73                7 / 73                7 / 73                7 / 73          ops/s\n" +
                        "[10.418s][info][gc,stats    ]      Memory: Undo Page Allocation                              0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s\n" +
                        "[10.418s][info][gc,stats    ]       Phase: Concurrent Mark                              45.361 / 66.984       45.361 / 66.984       45.361 / 66.984       45.361 / 66.984      ms\n" +
                        "[10.418s][info][gc,stats    ]       Phase: Concurrent Mark Continue                      0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[10.418s][info][gc,stats    ]       Phase: Concurrent Mark Free                          0.001 / 0.001         0.001 / 0.001         0.001 / 0.001         0.001 / 0.001       ms\n" +
                        "[10.418s][info][gc,stats    ]       Phase: Concurrent Process Non-Strong References      0.423 / 0.697         0.423 / 0.697         0.423 / 0.697         0.423 / 0.697       ms\n" +
                        "[10.418s][info][gc,stats    ]       Phase: Concurrent Relocate                           0.808 / 0.928         0.808 / 0.928         0.808 / 0.928         0.808 / 0.928       ms\n" +
                        "[10.418s][info][gc,stats    ]       Phase: Concurrent Reset Relocation Set               0.001 / 0.001         0.001 / 0.001         0.001 / 0.001         0.001 / 0.001       ms\n" +
                        "[10.418s][info][gc,stats    ]       Phase: Concurrent Select Relocation Set              4.461 / 8.843         4.461 / 8.843         4.461 / 8.843         4.461 / 8.843       ms\n" +
                        "[10.418s][info][gc,stats    ]       Phase: Pause Mark End                                0.018 / 0.031         0.018 / 0.031         0.018 / 0.031         0.018 / 0.031       ms\n" +
                        "[10.418s][info][gc,stats    ]       Phase: Pause Mark Start                              0.010 / 0.015         0.010 / 0.015         0.010 / 0.015         0.010 / 0.015       ms\n" +
                        "[10.418s][info][gc,stats    ]       Phase: Pause Relocate Start                          0.007 / 0.008         0.007 / 0.008         0.007 / 0.008         0.007 / 0.008       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent Classes Purge                      0.039 / 0.060         0.039 / 0.060         0.039 / 0.060         0.039 / 0.060       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent Classes Unlink                     0.097 / 0.143         0.097 / 0.143         0.097 / 0.143         0.097 / 0.143       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent Mark                              44.745 / 66.798       44.745 / 66.798       44.745 / 66.798       44.745 / 66.798      ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent Mark Try Flush                     0.185 / 0.349         0.185 / 0.349         0.185 / 0.349         0.185 / 0.349       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent Mark Try Terminate                 0.558 / 1.293         0.558 / 1.293         0.558 / 1.293         0.558 / 1.293       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent References Enqueue                 0.002 / 0.006         0.002 / 0.006         0.002 / 0.006         0.002 / 0.006       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent References Process                 0.020 / 0.034         0.020 / 0.034         0.020 / 0.034         0.020 / 0.034       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent Roots ClassLoaderDataGraph         0.058 / 0.161         0.058 / 0.161         0.058 / 0.161         0.058 / 0.161       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent Roots CodeCache                    0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent Roots JavaThreads                  0.100 / 0.147         0.100 / 0.147         0.100 / 0.147         0.100 / 0.147       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent Roots OopStorageSet                0.031 / 0.054         0.031 / 0.054         0.031 / 0.054         0.031 / 0.054       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Concurrent Weak Roots OopStorageSet           0.071 / 0.119         0.071 / 0.119         0.071 / 0.119         0.071 / 0.119       ms\n" +
                        "[10.418s][info][gc,stats    ]    Subphase: Pause Mark Try Complete                       0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms\n" +
                        "[10.418s][info][gc,stats    ]      System: Java Threads                                     11 / 11               11 / 11               11 / 11               11 / 11          threads\n" +
                        "[10.418s][info][gc,stats    ] =========================================================================================================================================================";
        UnifiedZGCLogParser parser = (UnifiedZGCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));
        ZGCModel model = (ZGCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 1);
        Assert.assertTrue(model.getGcModelMetadata().isMetaspaceCapacityReliable());
        GCEvent gc = model.getGcEvents().get(0);
        Assert.assertEquals(gc.getGcid(), 0);
        Assert.assertEquals(gc.getStartTime(), 918, DELTA);
        Assert.assertEquals(gc.getEndTime(), 950, DELTA);
        Assert.assertEquals(gc.getDuration(), 32, DELTA);
        Assert.assertEquals(gc.getEventType(), GCEventType.ZGC_GARBAGE_COLLECTION);
        Assert.assertEquals(gc.getCause(), WARMUP);
        Assert.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getStartTime(), 918 - 0.007, DELTA);
        Assert.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getDuration(), 0.007, DELTA);
        Assert.assertEquals(gc.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, -1, -1, 0, 0));
        Assert.assertEquals(gc.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 104L * 1024 * 1024, 1000L * 1024 * 1024, 88 * 1024 * 1024, 1000L * 1024 * 1024));
        Assert.assertEquals(gc.getAllocation(), 3 * 1024 * 1024);
        Assert.assertEquals(gc.getReclamation(), 19L * 1024 * 1024);

        List<ZGCModel.ZStatistics> statistics = model.getStatistics();
        Assert.assertEquals(statistics.size(), 1);
        Assert.assertEquals(44, statistics.get(0).getStatisticItems().size());
        Assert.assertEquals(statistics.get(0).getStartTime(), 10417, DELTA);
        Assert.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10s(), 11, DELTA);
        Assert.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10h(), 11, DELTA);
    }

    @Test
    public void testJDK17G1Parser() throws Exception {
        String log =
                "[0.020s][info][gc] Using G1\n" +
                        "[0.022s][info][gc,init] Version: 17.0.1+12-39 (release)\n" +
                        "[0.022s][info][gc,init] CPUs: 8 total, 8 available\n" +
                        "[0.022s][info][gc,init] Memory: 16384M\n" +
                        "[0.022s][info][gc,init] Large Page Support: Disabled\n" +
                        "[0.022s][info][gc,init] NUMA Support: Disabled\n" +
                        "[0.022s][info][gc,init] Compressed Oops: Enabled (Zero based)\n" +
                        "[0.022s][info][gc,init] Heap Region Size: 1M\n" +
                        "[0.022s][info][gc,init] Heap Min Capacity: 100M\n" +
                        "[0.022s][info][gc,init] Heap Initial Capacity: 100M\n" +
                        "[0.022s][info][gc,init] Heap Max Capacity: 100M\n" +
                        "[0.022s][info][gc,init] Pre-touch: Disabled\n" +
                        "[0.022s][info][gc,init] Parallel Workers: 8\n" +
                        "[0.022s][info][gc,init] Concurrent Workers: 2\n" +
                        "[0.022s][info][gc,init] Concurrent Refinement Workers: 8\n" +
                        "[0.022s][info][gc,init] Periodic GC: Disabled\n" +
                        "[0.024s][info][gc,metaspace] CDS archive(s) mapped at: [0x0000000800000000-0x0000000800bd4000-0x0000000800bd4000), size 12402688, SharedBaseAddress: 0x0000000800000000, ArchiveRelocationMode: 0.\n" +
                        "[0.025s][info][gc,metaspace] Compressed class space mapped at: 0x0000000800c00000-0x0000000840c00000, reserved size: 1073741824\n" +
                        "[0.025s][info][gc,metaspace] Narrow klass base: 0x0000000800000000, Narrow klass shift: 0, Narrow klass range: 0x100000000\n" +
                        "[0.333s][info][gc,start    ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)\n" +
                        "[0.333s][info][gc,task     ] GC(0) Using 2 workers of 8 for evacuation\n" +
                        "[0.354s][info][gc,phases   ] GC(0)   Pre Evacuate Collection Set: 0.0ms\n" +
                        "[0.354s][info][gc,phases   ] GC(0)   Merge Heap Roots: 0.1ms\n" +
                        "[0.354s][info][gc,phases   ] GC(0)   Evacuate Collection Set: 20.3ms\n" +
                        "[0.354s][info][gc,phases   ] GC(0)   Post Evacuate Collection Set: 0.2ms\n" +
                        "[0.354s][info][gc,phases   ] GC(0)   Other: 0.3ms\n" +
                        "[0.354s][info][gc,heap     ] GC(0) Eden regions: 50->0(43)\n" +
                        "[0.354s][info][gc,heap     ] GC(0) Survivor regions: 0->7(7)\n" +
                        "[0.354s][info][gc,heap     ] GC(0) Old regions: 0->18\n" +
                        "[0.354s][info][gc,heap     ] GC(0) Archive regions: 2->2\n" +
                        "[0.354s][info][gc,heap     ] GC(0) Humongous regions: 1->1\n" +
                        "[0.354s][info][gc,metaspace] GC(0) Metaspace: 87K(320K)->87K(320K) NonClass: 84K(192K)->84K(192K) Class: 3K(128K)->3K(128K)\n" +
                        "[0.354s][info][gc          ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 51M->26M(100M) 20.955ms\n" +
                        "[0.354s][info][gc,cpu      ] GC(0) User=0.03s Sys=0.01s Real=0.02s\n" +
                        "[1.097s][info][gc          ] GC(1) Concurrent Mark Cycle\n" +
                        "[1.097s][info][gc,marking  ] GC(1) Concurrent Clear Claimed Marks\n" +
                        "[1.097s][info][gc,marking  ] GC(1) Concurrent Clear Claimed Marks 0.020ms\n" +
                        "[1.097s][info][gc,marking  ] GC(1) Concurrent Scan Root Regions\n" +
                        "[1.099s][info][gc,marking  ] GC(1) Concurrent Scan Root Regions 1.966ms\n" +
                        "[1.099s][info][gc,marking  ] GC(1) Concurrent Mark\n" +
                        "[1.099s][info][gc,marking  ] GC(1) Concurrent Mark From Roots\n" +
                        "[1.099s][info][gc,task     ] GC(1) Using 2 workers of 2 for marking\n" +
                        "[1.113s][info][gc,marking  ] GC(1) Concurrent Mark From Roots 14.489ms\n" +
                        "[1.113s][info][gc,marking  ] GC(1) Concurrent Preclean\n" +
                        "[1.113s][info][gc,marking  ] GC(1) Concurrent Preclean 0.061ms\n" +
                        "[1.114s][info][gc,start    ] GC(1) Pause Remark\n" +
                        "[1.114s][info][gc          ] GC(1) Pause Remark 82M->65M(100M) 0.341ms\n" +
                        "[1.114s][info][gc,cpu      ] GC(1) User=0.00s Sys=0.00s Real=0.00s\n" +
                        "[1.115s][info][gc,marking  ] GC(1) Concurrent Mark 15.656ms\n" +
                        "[1.115s][info][gc,marking  ] GC(1) Concurrent Rebuild Remembered Sets\n" +
                        "[1.121s][info][gc,marking  ] GC(1) Concurrent Rebuild Remembered Sets 6.891ms\n" +
                        "[1.122s][info][gc,start    ] GC(1) Pause Cleanup\n" +
                        "[1.122s][info][gc          ] GC(1) Pause Cleanup 65M->65M(100M) 0.056ms\n" +
                        "[1.122s][info][gc,cpu      ] GC(1) User=0.00s Sys=0.00s Real=0.00s\n" +
                        "[1.122s][info][gc,marking  ] GC(1) Concurrent Cleanup for Next Mark\n" +
                        "[1.122s][info][gc,marking  ] GC(1) Concurrent Cleanup for Next Mark 0.417ms\n" +
                        "[1.122s][info][gc          ] GC(1) Concurrent Mark Cycle 25.265ms\n" +
                        "[1.715s][info][gc,start    ] GC(2) Pause Full (G1 Compaction Pause)\n" +
                        "[1.715s][info][gc,phases,start] GC(2) Phase 1: Mark live objects\n" +
                        "[1.729s][info][gc,phases      ] GC(2) Phase 1: Mark live objects 14.039ms\n" +
                        "[1.729s][info][gc,phases,start] GC(2) Phase 2: Prepare for compaction\n" +
                        "[1.730s][info][gc,phases      ] GC(2) Phase 2: Prepare for compaction 0.875ms\n" +
                        "[1.730s][info][gc,phases,start] GC(2) Phase 3: Adjust pointers\n" +
                        "[1.736s][info][gc,phases      ] GC(2) Phase 3: Adjust pointers 6.156ms\n" +
                        "[1.736s][info][gc,phases,start] GC(2) Phase 4: Compact heap\n" +
                        "[1.738s][info][gc,phases      ] GC(2) Phase 4: Compact heap 1.153ms\n" +
                        "[1.738s][info][gc,heap        ] GC(2) Eden regions: 0->0(50)\n" +
                        "[1.738s][info][gc,heap        ] GC(2) Survivor regions: 0->0(0)\n" +
                        "[1.738s][info][gc,heap        ] GC(2) Old regions: 96->68\n" +
                        "[1.738s][info][gc,heap        ] GC(2) Archive regions: 2->2\n" +
                        "[1.738s][info][gc,heap        ] GC(2) Humongous regions: 2->1\n" +
                        "[1.738s][info][gc,metaspace   ] GC(2) Metaspace: 87K(320K)->87K(320K) NonClass: 84K(192K)->84K(192K) Class: 3K(128K)->3K(128K)\n" +
                        "[1.738s][info][gc             ] GC(2) Pause Full (G1 Compaction Pause) 98M->69M(100M) 22.935ms\n" +
                        "[1.738s][info][gc,cpu         ] GC(2) User=0.04s Sys=0.00s Real=0.02s\n" +
                        "[2.145s][info][gc          ] GC(3) Concurrent Undo Cycle\n" +
                        "[2.145s][info][gc,marking  ] GC(3) Concurrent Cleanup for Next Mark\n" +
                        "[2.145s][info][gc,marking  ] GC(3) Concurrent Cleanup for Next Mark 0.109ms\n" +
                        "[2.145s][info][gc          ] GC(3) Concurrent Undo Cycle 0.125ms";

        UnifiedG1GCLogParser parser = (UnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));
        G1GCModel model = (G1GCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        // assert parsing success
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 4);
        Assert.assertEquals(model.getHeapRegionSize(), 1L * 1024 * 1024);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 333, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 20.955, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), G1_EVACUATION_PAUSE);
        Assert.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 50 * 1024 * 1024, UNKNOWN_INT, 7 * 1024 * 1024, 50 * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 50 * 1024 * 1024, UNKNOWN_INT, 0 * 1024 * 1024, 43 * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(SURVIVOR), new GCMemoryItem(SURVIVOR, 0 * 1024, UNKNOWN_INT, 7 * 1024 * 1024, 7 * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, UNKNOWN_INT, 18 * 1024 * 1024, 50 * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(ARCHIVE), new GCMemoryItem(ARCHIVE, 2 * 1024 * 1024, UNKNOWN_INT, 2 * 1024 * 1024, UNKNOWN_INT));
        Assert.assertEquals(youngGC.getMemoryItem(HUMONGOUS), new GCMemoryItem(HUMONGOUS, 1 * 1024 * 1024, UNKNOWN_INT, 1 * 1024 * 1024, UNKNOWN_INT));
        Assert.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 87 * 1024, 320 * 1024, 87 * 1024, 320 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(NONCLASS), new GCMemoryItem(NONCLASS, 84 * 1024, 192 * 1024, 84 * 1024, 192 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(CLASS), new GCMemoryItem(CLASS, 3 * 1024, 128 * 1024, 3 * 1024, 128 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 51 * 1024 * 1024, UNKNOWN_INT, 26 * 1024 * 1024, 100 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 20, DELTA);
        Assert.assertEquals(youngGC.getPhases().size(), 5);
        Assert.assertEquals(youngGC.getPhases().get(1).getEventType(), GCEventType.G1_MERGE_HEAP_ROOTS);
        Assert.assertEquals(youngGC.getPhases().get(1).getDuration(), 0.1, DELTA);
        for (GCEvent phase : youngGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() >= 0);
            Assert.assertTrue(phase.getDuration() >= 0);
        }

        GCEvent concurrentCycle = model.getGcEvents().get(1);
        Assert.assertEquals(concurrentCycle.getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assert.assertEquals(concurrentCycle.getGcid(), 1);
        Assert.assertEquals(concurrentCycle.getStartTime(), 1097, DELTA);
        Assert.assertEquals(concurrentCycle.getDuration(), 25.265, DELTA);
        Assert.assertEquals(concurrentCycle.getPhases().size(), 9);
        for (GCEvent phase : concurrentCycle.getPhases()) {
            Assert.assertTrue(phase.getStartTime() > 0);
            Assert.assertTrue(phase.getDuration() > 0);
        }

        GCEvent fullGC = model.getGcEvents().get(2);
        Assert.assertEquals(fullGC.getGcid(), 2);
        Assert.assertEquals(fullGC.getStartTime(), 1715, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 22.935, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), G1_COMPACTION);
        Assert.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 0 * 1024 * 1024, UNKNOWN_INT, 0 * 1024 * 1024, 50 * 1024 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 98 * 1024 * 1024, UNKNOWN_INT, 69 * 1024 * 1024, 100 * 1024 * 1024));
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 20, DELTA);
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() >= 0);
            Assert.assertTrue(phase.getDuration() >= 0);
        }

        GCEvent concurrentUndo = model.getGcEvents().get(3);
        Assert.assertEquals(concurrentUndo.getEventType(), GCEventType.G1_CONCURRENT_UNDO_CYCLE);
        Assert.assertEquals(concurrentUndo.getGcid(), 3);
        Assert.assertEquals(concurrentUndo.getStartTime(), 2145, DELTA);
        Assert.assertEquals(concurrentUndo.getDuration(), 0.125, DELTA);
        Assert.assertEquals(concurrentUndo.getPhases().size(), 1);
        for (GCEvent phase : concurrentUndo.getPhases()) {
            Assert.assertTrue(phase.getStartTime() > 0);
            Assert.assertTrue(phase.getDuration() > 0);
        }

        GCLogMetadata metadata = model.getGcModelMetadata();
        Assert.assertTrue(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS.getName()));
        Assert.assertTrue(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_UNDO_CYCLE.getName()));
    }

    @Test
    public void testJDK17G1InferRegionSize() throws Exception {
        String log =
                "[1.715s][info][gc,start    ] GC(2) Pause Full (G1 Compaction Pause)\n" +
                        "[1.715s][info][gc,phases,start] GC(2) Phase 1: Mark live objects\n" +
                        "[1.729s][info][gc,phases      ] GC(2) Phase 1: Mark live objects 14.039ms\n" +
                        "[1.729s][info][gc,phases,start] GC(2) Phase 2: Prepare for compaction\n" +
                        "[1.730s][info][gc,phases      ] GC(2) Phase 2: Prepare for compaction 0.875ms\n" +
                        "[1.730s][info][gc,phases,start] GC(2) Phase 3: Adjust pointers\n" +
                        "[1.736s][info][gc,phases      ] GC(2) Phase 3: Adjust pointers 6.156ms\n" +
                        "[1.736s][info][gc,phases,start] GC(2) Phase 4: Compact heap\n" +
                        "[1.738s][info][gc,phases      ] GC(2) Phase 4: Compact heap 1.153ms\n" +
                        "[1.738s][info][gc,heap        ] GC(2) Eden regions: 0->0(50)\n" +
                        "[1.738s][info][gc,heap        ] GC(2) Survivor regions: 0->0(0)\n" +
                        "[1.738s][info][gc,heap        ] GC(2) Old regions: 96->68\n" +
                        "[1.738s][info][gc,heap        ] GC(2) Archive regions: 2->2\n" +
                        "[1.738s][info][gc,heap        ] GC(2) Humongous regions: 2->1\n" +
                        "[1.738s][info][gc,metaspace   ] GC(2) Metaspace: 87K(320K)->87K(320K) NonClass: 84K(192K)->84K(192K) Class: 3K(128K)->3K(128K)\n" +
                        "[1.738s][info][gc             ] GC(2) Pause Full (G1 Compaction Pause) 98M->69M(100M) 22.935ms\n" +
                        "[1.738s][info][gc,cpu         ] GC(2) User=0.04s Sys=0.00s Real=0.02s";

        UnifiedG1GCLogParser parser = (UnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(stringToBufferedReader(log)));
        G1GCModel model = (G1GCModel) parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        // assert parsing success
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 1);
        Assert.assertEquals(model.getHeapRegionSize(), 1L * 1024 * 1024);
    }
}
