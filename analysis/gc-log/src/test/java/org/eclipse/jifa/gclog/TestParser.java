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

import org.eclipse.jifa.analysis.listener.DefaultProgressListener;
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.event.Safepoint;
import org.eclipse.jifa.gclog.event.ThreadEvent;
import org.eclipse.jifa.gclog.event.evnetInfo.GCMemoryItem;
import org.eclipse.jifa.gclog.model.CMSGCModel;
import org.eclipse.jifa.gclog.model.G1GCModel;
import org.eclipse.jifa.gclog.model.GCEventType;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.model.ParallelGCModel;
import org.eclipse.jifa.gclog.model.SerialGCModel;
import org.eclipse.jifa.gclog.model.ZGCModel;
import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.jifa.gclog.model.modeInfo.GCLogMetadata;
import org.eclipse.jifa.gclog.model.modeInfo.GCLogStyle;
import org.eclipse.jifa.gclog.parser.GCLogParser;
import org.eclipse.jifa.gclog.parser.GCLogParserFactory;
import org.eclipse.jifa.gclog.parser.GCLogParsingMetadata;
import org.eclipse.jifa.gclog.parser.PreUnifiedG1GCLogParser;
import org.eclipse.jifa.gclog.parser.PreUnifiedGenerationalGCLogParser;
import org.eclipse.jifa.gclog.parser.UnifiedG1GCLogParser;
import org.eclipse.jifa.gclog.parser.UnifiedGenerationalGCLogParser;
import org.eclipse.jifa.gclog.parser.UnifiedZGCLogParser;
import org.eclipse.jifa.gclog.vo.PauseStatistics;
import org.eclipse.jifa.gclog.vo.TimeRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.jifa.gclog.TestUtil.stringToBufferedReader;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.ALLOCATION_FAILURE;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.CMS_FINAL_REMARK;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.ERGONOMICS;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.G1_COMPACTION;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.G1_EVACUATION_PAUSE;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.HEAP_DUMP;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.METADATA_GENERATION_THRESHOLD;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.PROACTIVE;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.PROMOTION_FAILED;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.WARMUP;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType.TO_SPACE_EXHAUSTED;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType.YOUNG_GC_BECOME_FULL_GC;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.ARCHIVE;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.CLASS;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.EDEN;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.HEAP;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.HUMONGOUS;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.METASPACE;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.NONCLASS;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.OLD;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.SURVIVOR;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.YOUNG;
import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_DOUBLE;
import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_INT;

public class TestParser {

    public static final double DELTA = 1e-6;

    @Test
    public void testJDK11G1Parser() throws Exception {
        UnifiedG1GCLogParser parser = (UnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("11G1Parser.log")));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("11G1Parser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        // assert parsing success
        Assertions.assertNotNull(model);
        Assertions.assertEquals(model.getGcEvents().size(), 3);

        // assert model info
        Assertions.assertEquals(model.getStartTime(), 0.0 * 1000, DELTA);
        Assertions.assertEquals(model.getEndTime(), 7.123 * 1000, DELTA);
        Assertions.assertEquals(model.getCollectorType(), GCCollectorType.G1);

        Assertions.assertEquals(model.getHeapRegionSize(), 1024 * 1024);
        Assertions.assertNull(model.getVmOptions());
        Assertions.assertEquals(model.getParallelThread(), 8);
        Assertions.assertEquals(model.getConcurrentThread(), 2);

        // assert events correct
        Assertions.assertEquals(model.getSafepoints().size(), 1);

        Safepoint safepoint = model.getSafepoints().get(0);
        Assertions.assertEquals(safepoint.getStartTime(), 1010 - 10.1229, DELTA);
        Assertions.assertEquals(safepoint.getDuration(), 10.1229, DELTA);
        Assertions.assertEquals(safepoint.getTimeToEnter(), 0.0077, DELTA);

        List<GCEvent> event = model.getGcEvents();
        GCEvent youngGC = event.get(0);
        Assertions.assertEquals(youngGC.getGcid(), 0);
        Assertions.assertTrue(youngGC.isTrue(TO_SPACE_EXHAUSTED));
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getStartTime(), 1.0 * 1000, DELTA);
        Assertions.assertEquals(youngGC.getPause(), 10.709, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 10.709, DELTA);
        Assertions.assertEquals(youngGC.getCause(), METADATA_GENERATION_THRESHOLD);
        Assertions.assertEquals(youngGC.getCpuTime().getReal(), 0.01 * 1000, DELTA);
        Assertions.assertEquals(youngGC.getPhases().size(), 4);
        Assertions.assertEquals(youngGC.getPhases().get(1).getEventType(), GCEventType.G1_COLLECT_EVACUATION);
        Assertions.assertEquals(youngGC.getPhases().get(1).getDuration(), 9.5, DELTA);
        Assertions.assertEquals(youngGC.getMemoryItem(SURVIVOR)
                , new GCMemoryItem(SURVIVOR, 0 * 1024, 3 * 1024 * 1024, 3 * 1024 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(METASPACE)
                , new GCMemoryItem(METASPACE, 20679 * 1024, 20679 * 1024, 45056 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(HEAP)
                , new GCMemoryItem(HEAP, 19 * 1024 * 1024, 4 * 1024 * 1024, 64 * 1024 * 1024));
        Assertions.assertTrue(youngGC.toString().contains("To-space Exhausted"));

        GCEvent concurrentMark = event.get(1);
        Assertions.assertEquals(concurrentMark.getGcid(), 1);
        Assertions.assertEquals(concurrentMark.getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assertions.assertEquals(concurrentMark.getDuration(), 14.256, DELTA);
        Assertions.assertEquals(concurrentMark.getPause(), 2.381 + 0.094, DELTA);
        Assertions.assertEquals(concurrentMark.getPhases().get(0).getEventType(), GCEventType.G1_CONCURRENT_CLEAR_CLAIMED_MARKS);
        Assertions.assertEquals(concurrentMark.getPhases().get(0).getDuration(), 0.057, DELTA);

        GCEvent fullGC = event.get(2);
        Assertions.assertEquals(fullGC.getGcid(), 2);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getStartTime(), 7.055 * 1000, DELTA);
        Assertions.assertEquals(fullGC.getPause(), 67.806, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 67.806, DELTA);
        Assertions.assertEquals(fullGC.getCause(), G1_EVACUATION_PAUSE);
        Assertions.assertEquals(fullGC.getPhases().size(), 4);
        Assertions.assertEquals(fullGC.getPhases().get(3).getEventType(), GCEventType.G1_COMPACT_HEAP);
        Assertions.assertEquals(fullGC.getPhases().get(3).getDuration(), 57.656, DELTA);

        GCLogMetadata metadata = model.getGcModelMetadata();
        Assertions.assertTrue(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS.getName()));
        Assertions.assertFalse(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_UNDO_CYCLE.getName()));
    }

    @Test
    public void testJDK11G1ParserDetectHeapRegionSize() throws Exception {
        UnifiedG1GCLogParser parser = new UnifiedG1GCLogParser();
        parser.setMetadata(new GCLogParsingMetadata(GCCollectorType.G1, GCLogStyle.UNIFIED));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("11G1ParserDetectHeapRegionSize.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        // should infer region size 16m
        Assertions.assertEquals(model.getHeapRegionSize(), 16 * 1024 * 1024);
        Assertions.assertEquals(model.getGcEvents().get(0).getMemoryItem(OLD)
                , new GCMemoryItem(OLD, 32 * 16 * 1024 * 1024, 37 * 16 * 1024 * 1024, 1952L * 1024 * 1024));
        Assertions.assertEquals(model.getGcEvents().get(0).getMemoryItem(METASPACE)
                , new GCMemoryItem(METASPACE, 21709 * 1024, 21707 * 1024, 1069056 * 1024));
    }

    @Test
    public void testJDK11ParseDecoration() throws Exception {
        String log = "[2021-05-06T11:25:16.508+0800][info][gc           ] GC(0) Pause Young (Concurrent Start) (Metadata GC Threshold)\n" +
                "[2021-05-06T11:25:16.510+0800][info][gc           ] GC(1) Pause Young (Concurrent Start) (Metadata GC Threshold)\n";
        UnifiedG1GCLogParser parser = new UnifiedG1GCLogParser();
        parser.setMetadata(new GCLogParsingMetadata(GCCollectorType.G1, GCLogStyle.UNIFIED));
        GCModel model = parser.parse(stringToBufferedReader(log));
        Assertions.assertEquals(model.getReferenceTimestamp(), 1620271516508d, DELTA);
        Assertions.assertEquals(model.getGcEvents().get(1).getStartTime(), 2, DELTA);

        log = "[1000000000800ms][info][gc           ] GC(0) Pause Young (Concurrent Start) (Metadata GC Threshold)\n" +
                "[1000000000802ms][info][gc           ] GC(1) Pause Young (Concurrent Start) (Metadata GC Threshold)\n";
        parser = new UnifiedG1GCLogParser();
        parser.setMetadata(new GCLogParsingMetadata(GCCollectorType.G1, GCLogStyle.UNIFIED));
        model = parser.parse(stringToBufferedReader(log));
        Assertions.assertEquals(model.getReferenceTimestamp(), 1000000000800D, DELTA);
        Assertions.assertEquals(model.getGcEvents().get(1).getStartTime(), 2, DELTA);
    }

    @Test
    public void testJDK11ZGCParser() throws Exception {
        UnifiedZGCLogParser parser = (UnifiedZGCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("11ZGCParser.log")));
        ZGCModel model = (ZGCModel) parser.parse(TestUtil.getGCLog("11ZGCParser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);

        Assertions.assertEquals(model.getGcEvents().size(), 1);
        GCEvent gc = model.getGcEvents().get(0);
        Assertions.assertEquals(gc.getGcid(), 374);
        Assertions.assertEquals(gc.getStartTime(), 7000, DELTA);
        Assertions.assertEquals(gc.getEndTime(), 7356, DELTA);
        Assertions.assertEquals(gc.getDuration(), 356, DELTA);
        Assertions.assertEquals(gc.getEventType(), GCEventType.ZGC_GARBAGE_COLLECTION);
        Assertions.assertEquals(gc.getCause(), PROACTIVE);
        Assertions.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getEndTime(), 7006, DELTA);
        Assertions.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getDuration(), 4.459, DELTA);
        Assertions.assertEquals(gc.getMemoryItem(METASPACE).getPostCapacity(), 128 * 1024 * 1024);
        Assertions.assertEquals(gc.getMemoryItem(METASPACE).getPostUsed(), 125 * 1024 * 1024);
        Assertions.assertEquals(gc.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 5614L * 1024 * 1024, 40960L * 1024 * 1024, 1454 * 1024 * 1024, 40960L * 1024 * 1024));
        Assertions.assertEquals(gc.getAllocation(), 202 * 1024 * 1024);
        Assertions.assertEquals(gc.getReclamation(), 4200L * 1024 * 1024);

        List<ZGCModel.ZStatistics> statistics = model.getStatistics();
        Assertions.assertEquals(statistics.size(), 1);
        Assertions.assertEquals(72, statistics.get(0).getStatisticItems().size());
        Assertions.assertEquals(statistics.get(0).getStartTime(), 7555, DELTA);
        Assertions.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10s(), 911, DELTA);
        Assertions.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10h(), 913, DELTA);
        List<GCEvent> allocationStalls = model.getAllocationStalls();
        Assertions.assertEquals(allocationStalls.size(), 2);
        Assertions.assertEquals(allocationStalls.get(1).getEndTime(), 7888, DELTA);
        Assertions.assertEquals(allocationStalls.get(1).getDuration(), 0.391, DELTA);
        Assertions.assertEquals(((ThreadEvent) (allocationStalls.get(1))).getThreadName(), "NioProcessor-2");

        Assertions.assertEquals(model.getOoms().size(), 1);
        ThreadEvent oom = model.getOoms().get(0);
        Assertions.assertEquals(oom.getStartTime(), 7889, DELTA);
        Assertions.assertEquals(oom.getThreadName(), "thread 8");
    }

    @Test
    public void testJDK8CMSParser() throws Exception {
        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("8CMSParser.log")));

        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("8CMSParser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);

        Assertions.assertEquals(model.getGcEvents().size(), 10);
        Assertions.assertEquals(model.getSafepoints().size(), 1);

        Safepoint safepoint = model.getSafepoints().get(0);
        Assertions.assertEquals(safepoint.getStartTime(), 675110 - 0.1215, DELTA);
        Assertions.assertEquals(safepoint.getDuration(), 0.1215, DELTA);
        Assertions.assertEquals(safepoint.getTimeToEnter(), 0.0271, DELTA);

        GCEvent fullgc = model.getGcEvents().get(0);
        Assertions.assertEquals(fullgc.getStartTime(), 610956, DELTA);
        Assertions.assertEquals(fullgc.getDuration(), 1027.7002, DELTA);
        Assertions.assertEquals(fullgc.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullgc.getCause(), HEAP_DUMP);
        Assertions.assertEquals(fullgc.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 114217 * 1024, 113775 * 1024, 1153024 * 1024));
        Assertions.assertEquals(fullgc.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 1537414 * 1024, 1388294 * 1024, 4915200L * 1024));
        Assertions.assertEquals(fullgc.getMemoryItem(OLD), new GCMemoryItem(OLD, 324459 * 1024, 175339 * 1024, 3072000L * 1024));
        Assertions.assertEquals(fullgc.getPhases().size(), 4);
        Assertions.assertEquals(fullgc.getLastPhaseOfType(GCEventType.WEAK_REFS_PROCESSING).getStartTime(), 611637, DELTA);
        Assertions.assertEquals(fullgc.getLastPhaseOfType(GCEventType.WEAK_REFS_PROCESSING).getDuration(), 1.8945, DELTA);
        Assertions.assertEquals(fullgc.getCpuTime().getUser(), 1710, DELTA);
        Assertions.assertEquals(fullgc.getCpuTime().getSys(), 50, DELTA);
        Assertions.assertEquals(fullgc.getCpuTime().getReal(), 1030, DELTA);

        fullgc = model.getGcEvents().get(8);
        Assertions.assertEquals(fullgc.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullgc.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 3956586L * 1024, 1051300 * 1024, 4019584L * 1024));

        GCEvent youngGC = model.getGcEvents().get(9);
        Assertions.assertEquals(youngGC.getStartTime(), 813396, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 10.5137, DELTA);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 69952 * 1024, 11354 * 1024, 253440 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 69952 * 1024, 8704 * 1024, 78656 * 1024));
        Assertions.assertNull(youngGC.getPhases());
        Assertions.assertEquals(youngGC.getReferenceGC().getSoftReferenceStartTime(), 813404, DELTA);
        Assertions.assertEquals(youngGC.getReferenceGC().getSoftReferencePauseTime(), 0.0260, DELTA);
        Assertions.assertEquals(youngGC.getReferenceGC().getSoftReferenceCount(), 4);
        Assertions.assertEquals(youngGC.getReferenceGC().getWeakReferenceStartTime(), 813405, DELTA);
        Assertions.assertEquals(youngGC.getReferenceGC().getWeakReferencePauseTime(), 0.0110, DELTA);
        Assertions.assertEquals(youngGC.getReferenceGC().getWeakReferenceCount(), 59);
        Assertions.assertEquals(youngGC.getReferenceGC().getFinalReferenceStartTime(), 813406, DELTA);
        Assertions.assertEquals(youngGC.getReferenceGC().getFinalReferencePauseTime(), 2.5979, DELTA);
        Assertions.assertEquals(youngGC.getReferenceGC().getFinalReferenceCount(), 1407);
        Assertions.assertEquals(youngGC.getReferenceGC().getPhantomReferenceStartTime(), 813407, DELTA);
        Assertions.assertEquals(youngGC.getReferenceGC().getPhantomReferencePauseTime(), 0.0131, DELTA);
        Assertions.assertEquals(youngGC.getReferenceGC().getPhantomReferenceCount(), 11);
        Assertions.assertEquals(youngGC.getReferenceGC().getPhantomReferenceFreedCount(), 10);
        Assertions.assertEquals(youngGC.getReferenceGC().getJniWeakReferenceStartTime(), 813408, DELTA);
        Assertions.assertEquals(youngGC.getReferenceGC().getJniWeakReferencePauseTime(), 0.0088, DELTA);
        Assertions.assertEquals(youngGC.getCpuTime().getUser(), 40, DELTA);
        Assertions.assertEquals(youngGC.getCpuTime().getSys(), 10, DELTA);
        Assertions.assertEquals(youngGC.getCpuTime().getReal(), 10, DELTA);

        GCEvent cms = model.getGcEvents().get(2);
        Assertions.assertEquals(cms.getEventType(), GCEventType.CMS_CONCURRENT_MARK_SWEPT);
        Assertions.assertEquals(cms.getStartTime(), 675164, DELTA);
        Assertions.assertEquals(cms.getPhases().size(), 12, DELTA);
        for (GCEvent phase : cms.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assertions.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_INITIAL_MARK).getStartTime(), 675164, DELTA);
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getDuration(), 34415, DELTA);
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getCpuTime().getUser(), 154390, DELTA);
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 770, DELTA);
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getDuration(), 431.5, DELTA);
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_RESET).getDuration(), 237, DELTA);
    }

    @Test
    public void testJDK8CMSCPUTime() throws Exception {
        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("8CMSCPUTime.log")));

        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("8CMSCPUTime.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);
        Assertions.assertEquals(model.getLastEventOfType(GCEventType.CMS_INITIAL_MARK).getCpuTime().getReal(),130, DELTA);
        Assertions.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_MARK).getCpuTime().getUser(),10, DELTA);
        Assertions.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_PRECLEAN).getCpuTime().getReal(),30, DELTA);
        Assertions.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_ABORTABLE_PRECLEAN).getCpuTime().getReal(),4650, DELTA);
        Assertions.assertEquals(model.getLastEventOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getReal(),30, DELTA);
        Assertions.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_SWEEP).getCpuTime().getReal(),20, DELTA);
        Assertions.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_RESET).getCpuTime().getReal(),40, DELTA);
        Assertions.assertEquals(model.getLastEventOfType(GCEventType.YOUNG_GC).getCpuTime().getReal(),50, DELTA);
    }

    @Test
    public void testJDK8G1GCParser() throws Exception {
        PreUnifiedG1GCLogParser parser = (PreUnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("8G1GCParser.log")));

        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("8G1GCParser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);
        Assertions.assertEquals(model.getGcEvents().size(), 4);
        Assertions.assertEquals(model.getParallelThread(), 4);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assertions.assertEquals(youngGC.getStartTime(), 3960, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 56.3085, DELTA);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getCause(), G1_EVACUATION_PAUSE);
        Assertions.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 184 * 1024 * 1024, 3800L * 1024 * 1024, (int) (19.3 * 1024 * 1024), 3800L * 1024 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 184 * 1024 * 1024, 184 * 1024 * 1024, 0, 160 * 1024 * 1024));
        Assertions.assertNotNull(youngGC.getPhases());
        for (GCEvent phase : youngGC.getPhases()) {
            Assertions.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assertions.assertEquals(youngGC.getLastPhaseOfType(GCEventType.G1_GC_REFPROC).getDuration(), 15.1, DELTA);
        Assertions.assertEquals(youngGC.getLastPhaseOfType(GCEventType.G1_CODE_ROOT_SCANNING).getDuration(), 0.5, DELTA);
        Assertions.assertEquals(youngGC.getReferenceGC().getSoftReferenceStartTime(), 4000, DELTA);
        Assertions.assertEquals(youngGC.getReferenceGC().getJniWeakReferencePauseTime(), 0.0057, DELTA);
        Assertions.assertEquals(youngGC.getCpuTime().getUser(), 70, DELTA);
        Assertions.assertEquals(youngGC.getCpuTime().getSys(), 10, DELTA);
        Assertions.assertEquals(youngGC.getCpuTime().getReal(), 60, DELTA);

        GCEvent concurrentCycle = model.getGcEvents().get(1);
        Assertions.assertEquals(concurrentCycle.getStartTime(), 4230, DELTA);
        Assertions.assertEquals(concurrentCycle.getPhases().size(), 9);
        for (GCEvent phase : concurrentCycle.getPhases()) {
            if (phase.getEventType() != GCEventType.G1_CONCURRENT_MARK_RESET_FOR_OVERFLOW) {
                Assertions.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
            }
            Assertions.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
        }
        Assertions.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_CONCURRENT_SCAN_ROOT_REGIONS).getStartTime(), 4230, DELTA);
        Assertions.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_CONCURRENT_SCAN_ROOT_REGIONS).getDuration(), 160.8430, DELTA);
        Assertions.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_REMARK).getStartTime(), 19078, DELTA);
        Assertions.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_REMARK).getDuration(), 478.5858, DELTA);
        Assertions.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_PAUSE_CLEANUP).getMemoryItem(HEAP).getPostUsed(), 9863L * 1024 * 1024, DELTA);
        Assertions.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_REMARK).getCpuTime().getUser(), 1470, DELTA);
        Assertions.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_PAUSE_CLEANUP).getCpuTime().getSys(), 10, DELTA);

        GCEvent fullGC = model.getGcEvents().get(2);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getStartTime(), 23346, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 1924.2692, DELTA);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), METADATA_GENERATION_THRESHOLD);
        Assertions.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 1792694 * 1024, 291615 * 1024, 698368 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, (long) (7521.7 * 1024 * 1024), (long) (46144.0 * 1024 * 1024), (long) (7002.8 * 1024 * 1024), (long) (46144.0 * 1024 * 1024)));
        Assertions.assertEquals(fullGC.getCpuTime().getUser(), 2090, DELTA);
        Assertions.assertEquals(fullGC.getCpuTime().getSys(), 190, DELTA);
        Assertions.assertEquals(fullGC.getCpuTime().getReal(), 1920, DELTA);

        GCEvent mixedGC = model.getGcEvents().get(3);
        Assertions.assertEquals(mixedGC.getStartTime(), 79619, DELTA);
        Assertions.assertEquals(mixedGC.getDuration(), 26.4971, DELTA);
        Assertions.assertEquals(mixedGC.getEventType(), GCEventType.G1_MIXED_GC);
        Assertions.assertEquals(mixedGC.getCause(), G1_EVACUATION_PAUSE);
        Assertions.assertTrue(mixedGC.isTrue(TO_SPACE_EXHAUSTED));
        Assertions.assertEquals(mixedGC.getMemoryItem(HEAP).getPostCapacity(), (long) (19.8 * 1024 * 1024 * 1024));
        Assertions.assertEquals(mixedGC.getMemoryItem(EDEN).getPreUsed(), 2304L * 1024 * 1024);
        Assertions.assertNotNull(mixedGC.getPhases());
        for (GCEvent phase : mixedGC.getPhases()) {
            Assertions.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }

        GCLogMetadata metadata = model.getGcModelMetadata();
        Assertions.assertFalse(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS.getName()));
        Assertions.assertFalse(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_UNDO_CYCLE.getName()));
    }

    @Test
    public void testJDK8G1GCParserAdaptiveSize() throws Exception {
        // although we don't read anything from options like -XX:+PrintAdaptiveSizePolicy, they should not
        // affect parsing
        PreUnifiedG1GCLogParser parser = (PreUnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("8G1GCParserAdaptiveSize.log")));

        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("8G1GCParserAdaptiveSize.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);
        Assertions.assertEquals(model.getGcEvents().size(), 1);
        GCEvent youngGC = model.getGcEvents().get(0);
        Assertions.assertEquals(youngGC.getStartTime(), 683, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 8.5898, DELTA);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getCause(), G1_EVACUATION_PAUSE);
        Assertions.assertEquals(youngGC.getMemoryItem(HEAP).getPreUsed(), 52224 * 1024);
    }

    @Test
    public void testJDK11SerialGCParser() throws Exception {
        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("11SerialGCParser.log")));

        SerialGCModel model = (SerialGCModel) parser.parse(TestUtil.getGCLog("11SerialGCParser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);
        Assertions.assertEquals(model.getGcEvents().size(), 2);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assertions.assertEquals(youngGC.getGcid(), 0);
        Assertions.assertEquals(youngGC.getStartTime(), 486, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 25.164, DELTA);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 69952 * 1024, 8704 * 1024, 78656 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, 24185 * 1024, 174784 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 6529 * 1024, 6519 * 1024, 1056768 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 68 * 1024 * 1024, 32 * 1024 * 1024, 247 * 1024 * 1024));
        Assertions.assertEquals(youngGC.getCpuTime().getReal(), 20, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assertions.assertEquals(fullGC.getGcid(), 1);
        Assertions.assertEquals(fullGC.getStartTime(), 5614, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 146.617, DELTA);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 215 * 1024 * 1024, 132 * 1024 * 1024, 247 * 1024 * 1024));
        Assertions.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assertions.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 26.097, DELTA);
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 17.259, DELTA);
    }

    @Test
    public void testJDK11ParallelGCParser() throws Exception {
        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("11ParallelGCParser.log")));

        ParallelGCModel model = (ParallelGCModel) parser.parse(TestUtil.getGCLog("11ParallelGCParser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);
        Assertions.assertEquals(model.getGcEvents().size(), 2);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assertions.assertEquals(youngGC.getGcid(), 0);
        Assertions.assertEquals(youngGC.getStartTime(), 455, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 11.081, DELTA);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 65536 * 1024, 10720 * 1024, 76288 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, 20800 * 1024, 175104 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 6531 * 1024, 6531 * 1024, 1056768 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 64 * 1024 * 1024, 30 * 1024 * 1024, 245 * 1024 * 1024));
        Assertions.assertEquals(youngGC.getCpuTime().getReal(), 10, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assertions.assertEquals(fullGC.getGcid(), 1);
        Assertions.assertEquals(fullGC.getStartTime(), 2836, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 46.539, DELTA);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), ERGONOMICS);
        Assertions.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 10729 * 1024, 0, 76288 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 141664 * 1024, 94858 * 1024, 175104 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 7459 * 1024, 7459 * 1024, 1056768 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 148 * 1024 * 1024, 92 * 1024 * 1024, 245 * 1024 * 1024));
        Assertions.assertEquals(fullGC.getPhases().size(), 5);
        for (GCEvent phase : fullGC.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assertions.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_SUMMARY).getDuration(), 0.006, DELTA);
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_COMPACTION).getDuration(), 22.465, DELTA);
        Assertions.assertEquals(fullGC.getCpuTime().getReal(), 50, DELTA);
    }

    @Test
    public void testJDK11CMSGCParser() throws Exception {
        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("11CMSGCParser.log")));

        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("11CMSGCParser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);
        Assertions.assertEquals(model.getGcEvents().size(), 3);
        Assertions.assertEquals(model.getParallelThread(), 8);
        Assertions.assertEquals(model.getConcurrentThread(), 2);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assertions.assertEquals(youngGC.getGcid(), 0);
        Assertions.assertEquals(youngGC.getStartTime(), 479, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 31.208, DELTA);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 69952 * 1024, 8703 * 1024, 78656 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, 24072 * 1024, 174784 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 6531 * 1024, 6530 * 1024, 1056768 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 68 * 1024 * 1024, 32 * 1024 * 1024, 247 * 1024 * 1024));
        Assertions.assertEquals(youngGC.getCpuTime().getReal(), 30, DELTA);

        GCEvent cms = model.getGcEvents().get(1);
        Assertions.assertEquals(cms.getGcid(), 1);
        Assertions.assertEquals(cms.getEventType(), GCEventType.CMS_CONCURRENT_MARK_SWEPT);
        Assertions.assertEquals(cms.getStartTime(), 3231, DELTA);
        Assertions.assertEquals(cms.getPhases().size(), 6);
        for (GCEvent phase : cms.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assertions.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
            Assertions.assertNotNull(phase.getCpuTime());
            if (phase.getEventType() == GCEventType.CMS_INITIAL_MARK || phase.getEventType() == GCEventType.CMS_FINAL_REMARK) {
                Assertions.assertNotNull(phase.getMemoryItem(HEAP));
            }
        }
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_INITIAL_MARK).getStartTime(), 3231, DELTA);
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getDuration(), 22.229, DELTA);
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getCpuTime().getUser(), 70, DELTA);
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 20, DELTA);
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getDuration(), 1.991, DELTA);
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_RESET).getDuration(), 0.386, DELTA);
        Assertions.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_SWEEP).getMemoryItem(OLD), new GCMemoryItem(OLD, 142662 * 1024, 92308 * 1024, 174784 * 1024));

        GCEvent fullGC = model.getGcEvents().get(2);
        Assertions.assertEquals(fullGC.getGcid(), 2);
        Assertions.assertEquals(fullGC.getStartTime(), 8970, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 178.617, DELTA);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 174 * 1024 * 1024, 166 * 1024 * 1024, 247 * 1024 * 1024));
        Assertions.assertEquals(fullGC.getCpuTime().getReal(), 180, DELTA);
        Assertions.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assertions.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 24.761, DELTA);
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 28.069, DELTA);
    }

    @Test
    public void testJDK8ParallelGCParser() throws Exception {
        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("8ParallelGCParser.log")));

        ParallelGCModel model = (ParallelGCModel) parser.parse(TestUtil.getGCLog("8ParallelGCParser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);

        Assertions.assertEquals(model.getGcEvents().size(), 6);

        GCEvent youngGC = model.getGcEvents().get(2);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(youngGC.getStartTime(), 962, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 46.2864, DELTA);
        GCMemoryItem youngGen = youngGC.getMemoryItem(YOUNG);
        Assertions.assertEquals(youngGen.getPreUsed(), 51200 * 1024);
        Assertions.assertEquals(youngGen.getPostUsed(), 4096 * 1024);
        Assertions.assertEquals(youngGen.getPostCapacity(), 77824 * 1024);
        GCMemoryItem total = youngGC.getMemoryItem(HEAP);
        Assertions.assertEquals(total.getPreUsed(), 118572 * 1024);
        Assertions.assertEquals(total.getPostUsed(), 117625 * 1024);
        Assertions.assertEquals(total.getPostCapacity(), 252416 * 1024);
        Assertions.assertEquals(youngGC.getCpuTime().getUser(), 290, DELTA);

        GCEvent fullGC = model.getGcEvents().get(5);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), ERGONOMICS);
        Assertions.assertEquals(fullGC.getStartTime(), 14608, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 4.6781, DELTA);
        youngGen = fullGC.getMemoryItem(YOUNG);
        Assertions.assertEquals(youngGen.getPreUsed(), 65530 * 1024);
        Assertions.assertEquals(youngGen.getPostUsed(), 0);
        Assertions.assertEquals(youngGen.getPostCapacity(), 113664 * 1024);
        GCMemoryItem oldGen = fullGC.getMemoryItem(OLD);
        Assertions.assertEquals(oldGen.getPreUsed(), 341228 * 1024);
        Assertions.assertEquals(oldGen.getPostUsed(), 720 * 1024);
        Assertions.assertEquals(oldGen.getPostCapacity(), 302592 * 1024);
        GCMemoryItem metaspace = fullGC.getMemoryItem(METASPACE);
        Assertions.assertEquals(metaspace.getPreUsed(), 3740 * 1024);
        Assertions.assertEquals(metaspace.getPostUsed(), 3737 * 1024);
        Assertions.assertEquals(metaspace.getPostCapacity(), 1056768 * 1024);
        total = fullGC.getMemoryItem(HEAP);
        Assertions.assertEquals(total.getPreUsed(), 406759 * 1024);
        Assertions.assertEquals(total.getPostUsed(), 720 * 1024);
        Assertions.assertEquals(total.getPostCapacity(), 416256 * 1024);
        Assertions.assertEquals(fullGC.getCpuTime().getUser(), 20, DELTA);
    }

    @Test
    public void testJDK8SerialGCParser() throws Exception {

        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("8SerialGCParser.log")));

        SerialGCModel model = (SerialGCModel) parser.parse(TestUtil.getGCLog("8SerialGCParser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);

        Assertions.assertEquals(model.getGcEvents().size(), 8);
        Assertions.assertEquals(model.getReferenceTimestamp(), 1638847091688.0, DELTA);

        GCEvent youngGC = model.getGcEvents().get(1);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(youngGC.getStartTime(), 68, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 70.1086, DELTA);
        GCMemoryItem youngGen = youngGC.getMemoryItem(YOUNG);
        Assertions.assertEquals(youngGen.getPreUsed(), 78656 * 1024);
        Assertions.assertEquals(youngGen.getPostUsed(), 8703 * 1024);
        Assertions.assertEquals(youngGen.getPostCapacity(), 78656 * 1024);
        GCMemoryItem total = youngGC.getMemoryItem(HEAP);
        Assertions.assertEquals(total.getPreUsed(), 126740 * 1024);
        Assertions.assertEquals(total.getPostUsed(), 114869 * 1024);
        Assertions.assertEquals(total.getPostCapacity(), 253440 * 1024);
        Assertions.assertEquals(youngGC.getCpuTime().getReal(), 70, DELTA);

        GCEvent fullGC = model.getGcEvents().get(6);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(fullGC.getStartTime(), 1472, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 1095.987, DELTA);
        youngGen = fullGC.getMemoryItem(YOUNG);
        Assertions.assertEquals(youngGen.getPreUsed(), 271999 * 1024);
        Assertions.assertEquals(youngGen.getPostUsed(), 0);
        Assertions.assertEquals(youngGen.getPostCapacity(), 272000 * 1024);
        GCMemoryItem oldGen = fullGC.getMemoryItem(OLD);
        Assertions.assertEquals(oldGen.getPreUsed(), 785946 * 1024);
        Assertions.assertEquals(oldGen.getPostUsed(), 756062 * 1024);
        Assertions.assertEquals(oldGen.getPostCapacity(), 786120 * 1024);
        GCMemoryItem metaspace = fullGC.getMemoryItem(METASPACE);
        Assertions.assertEquals(metaspace.getPreUsed(), 3782 * 1024);
        Assertions.assertEquals(metaspace.getPostUsed(), 3782 * 1024);
        Assertions.assertEquals(metaspace.getPostCapacity(), 1056768 * 1024);
        total = fullGC.getMemoryItem(HEAP);
        Assertions.assertEquals(total.getPreUsed(), 823069 * 1024);
        Assertions.assertEquals(total.getPostUsed(), 756062 * 1024);
        Assertions.assertEquals(total.getPostCapacity(), 1058120 * 1024);
        Assertions.assertEquals(fullGC.getCpuTime().getSys(), 70, DELTA);
    }

    @Test
    public void testJDK8GenerationalGCInterleave() throws Exception {
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8GenerationalGCInterleave.log"));
        GCModel model = parser.parse(TestUtil.getGCLog("8GenerationalGCInterleave.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);

        Assertions.assertEquals(model.getGcEvents().size(), 1);
        GCEvent fullGC = model.getGcEvents().get(0);
        Assertions.assertEquals(fullGC.getStartTime(), 61988328, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 2016.0411, DELTA);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 2621440L * 1024, 0, 2883584L * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 1341593L * 1024, 1329988L * 1024, 2097152L * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 3963033L * 1024, 1329988L * 1024, 4980736L * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 310050L * 1024, 309844L * 1024, 1343488L * 1024));
        Assertions.assertEquals(fullGC.getCpuTime().getReal(), 2010, DELTA);
    }


    @Test
    public void testJDK11GenerationalGCInterleave() throws Exception {

        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("11GenerationalGCInterleave.log")));

        SerialGCModel model = (SerialGCModel) parser.parse(TestUtil.getGCLog("11GenerationalGCInterleave.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);

        Assertions.assertEquals(model.getGcEvents().size(), 1);
        GCEvent fullGC = model.getGcEvents().get(0);
        Assertions.assertEquals(fullGC.getGcid(), 3);
        Assertions.assertEquals(fullGC.getStartTime(), 5643, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 146.211, DELTA);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 78655 * 1024, 0, 78656 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 142112 * 1024, 135957 * 1024, 174784 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 215 * 1024 * 1024, 132 * 1024 * 1024, 247 * 1024 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 7462 * 1024, 7462 * 1024, 1056768 * 1024));
        Assertions.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assertions.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 24.314, DELTA);
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 17.974, DELTA);
        Assertions.assertTrue(fullGC.isTrue(YOUNG_GC_BECOME_FULL_GC));
    }

    @Test
    public void TestIncompleteGCLog() throws Exception {
        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("IncompleteGCLog.log")));

        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("IncompleteGCLog.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);

        Assertions.assertEquals(model.getGcEvents().size(), 2);
        Assertions.assertEquals(model.getAllEvents().size(), 8);
    }

    @Test
    public void testJDK8ConcurrentPrintDateTimeStamp() throws Exception {
        /*
         * This test mainly test the line below. The format here is:
         * [DateStamp] [DateStamp] [TimeStamp] [TimeStamp] [Safepoint]
         * [Concurrent cycle phase]
         */
        PreUnifiedG1GCLogParser parser = (PreUnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("8ConcurrentPrintDateTimeStamp.log")));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("8ConcurrentPrintDateTimeStamp.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertEquals(model.getGcEvents().size(), 3);
        Assertions.assertEquals(model.getGcEvents().get(0).getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(model.getGcEvents().get(1).getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assertions.assertEquals(model.getGcEvents().get(1).getPhases().get(0).getEventType(), GCEventType.G1_CONCURRENT_SCAN_ROOT_REGIONS);
        Assertions.assertEquals(model.getGcEvents().get(1).getPhases().get(0).getStartTime(), 725081, DELTA);
        Assertions.assertEquals(model.getGcEvents().get(2).getEventType(), GCEventType.YOUNG_GC);
    }

    @Test
    public void TestJDK8CMSPromotionFailed() throws Exception {
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8CMSPromotionFailed.log"));
        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("8CMSPromotionFailed.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assertions.assertEquals(model.getGcEvents().size(), 1);
        GCEvent fullGC = model.getGcEvents().get(0);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 7689600L * 1024, 0, 7689600L * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 9258265L * 1024, 5393434L * 1024, 12582912L * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 16878013L * 1024, 5393434L * 1024, 20272512L * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 208055 * 1024, 203568 * 1024, 1253376 * 1024));
        Assertions.assertEquals(fullGC.getCause(), PROMOTION_FAILED);
        Assertions.assertTrue(fullGC.isTrue(YOUNG_GC_BECOME_FULL_GC));

    }

    @Test
    public void TestJDK8CMSScavengeBeforeRemark() throws Exception {
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8CMSScavengeBeforeRemark.log"));
        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("8CMSScavengeBeforeRemark.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assertions.assertEquals(model.getGcEvents().size(), 3);
        Assertions.assertEquals(model.getLastEventOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 830, DELTA);

        GCEvent youngGC = model.getGcEvents().get(1);
        Assertions.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 1922431L * 1024L, 174720L * 1024L, 1922432L * 1024L));

        youngGC = model.getGcEvents().get(2);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 1056998L * 1024L, 151245L * 1024L, 1922432L * 1024L));
        Assertions.assertEquals(youngGC.getCpuTime().getUser(), 490, DELTA);
        Assertions.assertEquals(youngGC.getCause(), CMS_FINAL_REMARK);

        PauseStatistics pause = model.getPauseStatistics(new TimeRange(0, 99999999));
        Assertions.assertEquals(pause.getPauseAvg(), (226.6431 + 362.9243 + 293.1600) / 3.0, DELTA);
    }

    @Test
    public void TestJDK11CMSScavengeBeforeRemark() throws Exception {
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("11CMSScavengeBeforeRemark.log"));
        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("11CMSScavengeBeforeRemark.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assertions.assertEquals(model.getGcEvents().size(), 3);
        Assertions.assertEquals(model.getLastEventOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 90, DELTA);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assertions.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 46079 * 1024L, 5120 * 1024L, 46080 * 1024L));

        youngGC = model.getGcEvents().get(2);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 5923 * 1024L, 5921 * 1024L, 46080 * 1024L));
        Assertions.assertEquals(youngGC.getCpuTime().getUser(), 70, DELTA);
        Assertions.assertEquals(youngGC.getCause(), CMS_FINAL_REMARK);

        PauseStatistics pause = model.getPauseStatistics(new TimeRange(0, 99999999));
        Assertions.assertEquals(pause.getPauseAvg(), (32.662 + 3.259 + 2.784) / 3.0, DELTA);
    }

    @Test
    public void TestJDK17SerialGCParser() throws Exception {
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("17SerialGCParser.log"));
        SerialGCModel model = (SerialGCModel) parser.parse(TestUtil.getGCLog("17SerialGCParser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());

        GCEvent youngGC = model.getGcEvents().get(0);
        Assertions.assertEquals(youngGC.getGcid(), 0);
        Assertions.assertEquals(youngGC.getStartTime(), 173, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 21.766, DELTA);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 40960 * 1024, 46080 * 1024, 5120 * 1024, 46080 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 40960 * 1024, 40960 * 1024, 0 * 1024, 40960 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(SURVIVOR), new GCMemoryItem(SURVIVOR, 0 * 1024, 5120 * 1024, 5120 * 1024, 5120 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, 51200 * 1024, 14524 * 1024, 51200 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 137 * 1024, 384 * 1024, 138 * 1024, 384 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(NONCLASS), new GCMemoryItem(NONCLASS, 133 * 1024, 256 * 1024, 134 * 1024, 256 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(CLASS), new GCMemoryItem(CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 40 * 1024 * 1024, 95 * 1024 * 1024, 19 * 1024 * 1024, 95 * 1024 * 1024));
        Assertions.assertEquals(youngGC.getCpuTime().getReal(), 20, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assertions.assertEquals(fullGC.getGcid(), 1);
        Assertions.assertEquals(fullGC.getStartTime(), 2616, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 92.137, DELTA);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 46079 * 1024, 46080 * 1024, 36798 * 1024, 46080 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 40960 * 1024, 40960 * 1024, 36798 * 1024, 40960 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(SURVIVOR), new GCMemoryItem(SURVIVOR, 5119 * 1024, 5120 * 1024, 0 * 1024, 5120 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 51199 * 1024, 51200 * 1024, 51199 * 1024, 51200 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 137 * 1024, 384 * 1024, 137 * 1024, 384 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(NONCLASS), new GCMemoryItem(NONCLASS, 133 * 1024, 256 * 1024, 133 * 1024, 256 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(CLASS), new GCMemoryItem(CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 94 * 1024 * 1024, 95 * 1024 * 1024, 85 * 1024 * 1024, 95 * 1024 * 1024));
        Assertions.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assertions.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 12.103, DELTA);
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 10.313, DELTA);
    }

    @Test
    public void TestJDK17ParallelGCParser() throws Exception {
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("17ParallelGCParser.log"));
        ParallelGCModel model = (ParallelGCModel) parser.parse(TestUtil.getGCLog("17ParallelGCParser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());

        GCEvent youngGC = model.getGcEvents().get(0);
        Assertions.assertEquals(youngGC.getGcid(), 0);
        Assertions.assertEquals(youngGC.getStartTime(), 222, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 10.085, DELTA);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 38912 * 1024, 45056 * 1024, 6137 * 1024, 45056 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 38912 * 1024, 38912 * 1024, 0 * 1024, 38912 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(SURVIVOR), new GCMemoryItem(SURVIVOR, 0 * 1024, 6144 * 1024, 6137 * 1024, 6144 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, 51200 * 1024, 13208 * 1024, 51200 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 135 * 1024, 384 * 1024, 135 * 1024, 384 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(NONCLASS), new GCMemoryItem(NONCLASS, 131 * 1024, 256 * 1024, 131 * 1024, 256 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(CLASS), new GCMemoryItem(CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 38 * 1024 * 1024, (45056 + 51200) * 1024, 18 * 1024 * 1024, 94 * 1024 * 1024));
        Assertions.assertEquals(youngGC.getCpuTime().getReal(), 10, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assertions.assertEquals(fullGC.getGcid(), 1);
        Assertions.assertEquals(fullGC.getStartTime(), 547, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 21.046, DELTA);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), ERGONOMICS);
        Assertions.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 6128 * 1024, 45056 * 1024, 0 * 1024, 45056 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 0 * 1024, 38912 * 1024, 0 * 1024, 38912 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(SURVIVOR), new GCMemoryItem(SURVIVOR, 6128 * 1024, 6144 * 1024, 0 * 1024, 6144 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 46504 * 1024, 51200 * 1024, 38169 * 1024, 51200 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 135 * 1024, 384 * 1024, 135 * 1024, 384 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(NONCLASS), new GCMemoryItem(NONCLASS, 131 * 1024, 256 * 1024, 131 * 1024, 256 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(CLASS), new GCMemoryItem(CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 51 * 1024 * 1024, (45056 + 51200) * 1024, 37 * 1024 * 1024, 94 * 1024 * 1024));
        Assertions.assertEquals(fullGC.getPhases().size(), 5);
        for (GCEvent phase : fullGC.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assertions.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_SUMMARY).getDuration(), 0.006, DELTA);
        Assertions.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_COMPACTION).getDuration(), 6.917, DELTA);
        Assertions.assertEquals(fullGC.getCpuTime().getReal(), 20, DELTA);
    }

    @Test
    public void testJDK17ZGCParser() throws Exception {
        UnifiedZGCLogParser parser = (UnifiedZGCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("17ZGCParser.log")));
        ZGCModel model = (ZGCModel) parser.parse(TestUtil.getGCLog("17ZGCParser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assertions.assertNotNull(model);

        Assertions.assertEquals(model.getGcEvents().size(), 1);
        Assertions.assertTrue(model.getGcModelMetadata().isMetaspaceCapacityReliable());
        GCEvent gc = model.getGcEvents().get(0);
        Assertions.assertEquals(gc.getGcid(), 0);
        Assertions.assertEquals(gc.getStartTime(), 918, DELTA);
        Assertions.assertEquals(gc.getEndTime(), 950, DELTA);
        Assertions.assertEquals(gc.getDuration(), 32, DELTA);
        Assertions.assertEquals(gc.getEventType(), GCEventType.ZGC_GARBAGE_COLLECTION);
        Assertions.assertEquals(gc.getCause(), WARMUP);
        Assertions.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getStartTime(), 918 - 0.007, DELTA);
        Assertions.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getDuration(), 0.007, DELTA);
        Assertions.assertEquals(gc.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, -1, -1, 0, 0));
        Assertions.assertEquals(gc.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 104L * 1024 * 1024, 1000L * 1024 * 1024, 88 * 1024 * 1024, 1000L * 1024 * 1024));
        Assertions.assertEquals(gc.getAllocation(), 3 * 1024 * 1024);
        Assertions.assertEquals(gc.getReclamation(), 19L * 1024 * 1024);

        List<ZGCModel.ZStatistics> statistics = model.getStatistics();
        Assertions.assertEquals(statistics.size(), 1);
        Assertions.assertEquals(44, statistics.get(0).getStatisticItems().size());
        Assertions.assertEquals(statistics.get(0).getStartTime(), 10417, DELTA);
        Assertions.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10s(), 11, DELTA);
        Assertions.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10h(), 11, DELTA);
    }

    @Test
    public void testJDK17G1Parser() throws Exception {

        UnifiedG1GCLogParser parser = (UnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("17G1Parser.log")));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("17G1Parser.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        // assert parsing success
        Assertions.assertNotNull(model);
        Assertions.assertEquals(model.getGcEvents().size(), 4);
        Assertions.assertEquals(model.getHeapRegionSize(), 1L * 1024 * 1024);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assertions.assertEquals(youngGC.getGcid(), 0);
        Assertions.assertEquals(youngGC.getStartTime(), 333, DELTA);
        Assertions.assertEquals(youngGC.getDuration(), 20.955, DELTA);
        Assertions.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(youngGC.getCause(), G1_EVACUATION_PAUSE);
        Assertions.assertEquals(youngGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 50 * 1024 * 1024, UNKNOWN_INT, 7 * 1024 * 1024, 50 * 1024 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(EDEN), new GCMemoryItem(EDEN, 50 * 1024 * 1024, UNKNOWN_INT, 0 * 1024 * 1024, 43 * 1024 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(SURVIVOR), new GCMemoryItem(SURVIVOR, 0 * 1024, UNKNOWN_INT, 7 * 1024 * 1024, 7 * 1024 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(OLD), new GCMemoryItem(OLD, 0, UNKNOWN_INT, 18 * 1024 * 1024, 50 * 1024 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(ARCHIVE), new GCMemoryItem(ARCHIVE, 2 * 1024 * 1024, UNKNOWN_INT, 2 * 1024 * 1024, UNKNOWN_INT));
        Assertions.assertEquals(youngGC.getMemoryItem(HUMONGOUS), new GCMemoryItem(HUMONGOUS, 1 * 1024 * 1024, UNKNOWN_INT, 1 * 1024 * 1024, UNKNOWN_INT));
        Assertions.assertEquals(youngGC.getMemoryItem(METASPACE), new GCMemoryItem(METASPACE, 87 * 1024, 320 * 1024, 87 * 1024, 320 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(NONCLASS), new GCMemoryItem(NONCLASS, 84 * 1024, 192 * 1024, 84 * 1024, 192 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(CLASS), new GCMemoryItem(CLASS, 3 * 1024, 128 * 1024, 3 * 1024, 128 * 1024));
        Assertions.assertEquals(youngGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 51 * 1024 * 1024, UNKNOWN_INT, 26 * 1024 * 1024, 100 * 1024 * 1024));
        Assertions.assertEquals(youngGC.getCpuTime().getReal(), 20, DELTA);
        Assertions.assertEquals(youngGC.getPhases().size(), 5);
        Assertions.assertEquals(youngGC.getPhases().get(1).getEventType(), GCEventType.G1_MERGE_HEAP_ROOTS);
        Assertions.assertEquals(youngGC.getPhases().get(1).getDuration(), 0.1, DELTA);
        for (GCEvent phase : youngGC.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() >= 0);
            Assertions.assertTrue(phase.getDuration() >= 0);
        }

        GCEvent concurrentCycle = model.getGcEvents().get(1);
        Assertions.assertEquals(concurrentCycle.getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assertions.assertEquals(concurrentCycle.getGcid(), 1);
        Assertions.assertEquals(concurrentCycle.getStartTime(), 1097, DELTA);
        Assertions.assertEquals(concurrentCycle.getDuration(), 25.265, DELTA);
        Assertions.assertEquals(concurrentCycle.getPhases().size(), 9);
        for (GCEvent phase : concurrentCycle.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() > 0);
            Assertions.assertTrue(phase.getDuration() > 0);
        }

        GCEvent fullGC = model.getGcEvents().get(2);
        Assertions.assertEquals(fullGC.getGcid(), 2);
        Assertions.assertEquals(fullGC.getStartTime(), 1715, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 22.935, DELTA);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), G1_COMPACTION);
        Assertions.assertEquals(fullGC.getMemoryItem(YOUNG), new GCMemoryItem(YOUNG, 0 * 1024 * 1024, UNKNOWN_INT, 0 * 1024 * 1024, 50 * 1024 * 1024));
        Assertions.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 98 * 1024 * 1024, UNKNOWN_INT, 69 * 1024 * 1024, 100 * 1024 * 1024));
        Assertions.assertEquals(fullGC.getCpuTime().getReal(), 20, DELTA);
        Assertions.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() >= 0);
            Assertions.assertTrue(phase.getDuration() >= 0);
        }

        GCEvent concurrentUndo = model.getGcEvents().get(3);
        Assertions.assertEquals(concurrentUndo.getEventType(), GCEventType.G1_CONCURRENT_UNDO_CYCLE);
        Assertions.assertEquals(concurrentUndo.getGcid(), 3);
        Assertions.assertEquals(concurrentUndo.getStartTime(), 2145, DELTA);
        Assertions.assertEquals(concurrentUndo.getDuration(), 0.125, DELTA);
        Assertions.assertEquals(concurrentUndo.getPhases().size(), 1);
        for (GCEvent phase : concurrentUndo.getPhases()) {
            Assertions.assertTrue(phase.getStartTime() > 0);
            Assertions.assertTrue(phase.getDuration() > 0);
        }

        GCLogMetadata metadata = model.getGcModelMetadata();
        Assertions.assertTrue(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS.getName()));
        Assertions.assertTrue(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_UNDO_CYCLE.getName()));
    }

    @Test
    public void testJDK17G1InferRegionSize() throws Exception {

        UnifiedG1GCLogParser parser = (UnifiedG1GCLogParser)
                (new GCLogParserFactory().getParser(TestUtil.getGCLog("17G1InferRegionSize.log")));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("17G1InferRegionSize.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());
        // assert parsing success
        Assertions.assertNotNull(model);
        Assertions.assertEquals(model.getGcEvents().size(), 1);
        Assertions.assertEquals(model.getHeapRegionSize(), 1L * 1024 * 1024);
    }


    @Test
    public void testJDK8G1PrintGC() throws Exception {

        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8G1PrintGC.log"));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("8G1PrintGC.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isYoungGC).count(), 8);
        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isOldGC).count(), 2);
        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isFullGC).count(), 1);
        Assertions.assertEquals(model.getGcEvents().stream().filter(event -> event.isTrue(TO_SPACE_EXHAUSTED)).count(), 2);
        for (GCEvent event : model.getGcEvents()) {
            Assertions.assertTrue(event.getStartTime() > 0);
            if (event.isYoungGC() || event.isFullGC()) {
                Assertions.assertTrue(event.getDuration() > 0);
                Assertions.assertNotNull(event.getCause());
                Assertions.assertNotNull(event.getMemoryItem(HEAP));
            }
            if (event.isOldGC()) {
                Assertions.assertTrue( event.getPhases().size() >= 2);
            }
        }
    }

    @Test
    public void testJDK8CMSPrintGC() throws Exception {

        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8CMSPrintGC.log"));
        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("8CMSPrintGC.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isYoungGC).count(), 3);
        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isOldGC).count(), 4);
        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isFullGC).count(), 3);
        for (GCEvent event : model.getGcEvents()) {
            Assertions.assertTrue(event.getStartTime() > 0);
            if (event.isYoungGC() || event.isFullGC()) {
                Assertions.assertTrue(event.getDuration() > 0);
                Assertions.assertNotNull(event.getCause());
                Assertions.assertNotNull(event.getMemoryItem(HEAP));
            }
            if (event.isOldGC()) {
                Assertions.assertEquals(2, event.getPhases().size());
            }
        }
    }

    public void testJDK11G1PrintGC() throws Exception {

        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("11G1PrintGC.log"));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("11G1PrintGC.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isYoungGC).count(), 7);
        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isOldGC).count(), 1);
        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isFullGC).count(), 0);
        Assertions.assertEquals(model.getGcEvents().stream().filter(event -> event.isTrue(TO_SPACE_EXHAUSTED)).count(), 5);
        for (GCEvent event : model.getGcEvents()) {
            Assertions.assertTrue(event.getStartTime() > 0);
            if (event.isYoungGC() || event.isFullGC()) {
                Assertions.assertTrue(event.getDuration() > 0);
                Assertions.assertNotNull(event.getCause());
                Assertions.assertNotNull(event.getMemoryItem(HEAP));
            }
            if (event.isOldGC()) {
                Assertions.assertEquals(2, event.getPhases().size());
            }
        }
    }

    @Test
    public void testJDK11CMSPrintGC() throws Exception {

        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("11CMSPrintGC.log"));
        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("11CMSPrintGC.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isYoungGC).count(), 5);
        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isOldGC).count(), 3);
        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isFullGC).count(), 1);
        for (GCEvent event : model.getGcEvents()) {
            Assertions.assertTrue(event.getStartTime() > 0);
            if (event.isYoungGC() || event.isFullGC()) {
                Assertions.assertTrue(event.getDuration() > 0);
                Assertions.assertNotNull(event.getCause());
                Assertions.assertNotNull(event.getMemoryItem(HEAP));
            }
            if (event.isOldGC()) {
                Assertions.assertEquals(7, event.getPhases().size());
            }
        }
    }

    @Test
    public void testJDK8G1LogConcurrencyProblem() throws Exception {

        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8G1LogConcurrencyProblem.log"));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("8G1LogConcurrencyProblem.log"));
        model.calculateDerivedInfo(new DefaultProgressListener());

        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isYoungGC).count(), 2);
        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isOldGC).count(), 1);
        Assertions.assertEquals(model.getGcEvents().stream().filter(GCEvent::isFullGC).count(), 0);
        for (GCEvent event : model.getGcEvents()) {
            Assertions.assertTrue(event.getStartTime() > 0);
            if (event.isYoungGC() || event.isFullGC()) {
                Assertions.assertTrue(event.getDuration() > 0);
                Assertions.assertNotNull(event.getCause());
                Assertions.assertNotNull(event.getMemoryItem(HEAP));
            }
            if (event.isOldGC()) {
                Assertions.assertEquals(8, event.getPhases().size());
            }
        }
    }
}
