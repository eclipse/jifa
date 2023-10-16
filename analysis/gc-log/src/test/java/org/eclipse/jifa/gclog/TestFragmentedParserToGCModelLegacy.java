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
package org.eclipse.jifa.gclog;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.event.Safepoint;
import org.eclipse.jifa.gclog.event.evnetInfo.GCMemoryItem;
import org.eclipse.jifa.gclog.fragment.GCLogAnalyzer;
import org.eclipse.jifa.gclog.model.*;
import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.jifa.gclog.model.modeInfo.GCLogMetadata;
import org.eclipse.jifa.gclog.vo.PauseStatistics;
import org.eclipse.jifa.gclog.vo.TimeRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.*;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCCause.ALLOCATION_FAILURE;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType.TO_SPACE_EXHAUSTED;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType.YOUNG_GC_BECOME_FULL_GC;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.*;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.HEAP;
import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_DOUBLE;

/*
This test borrowed assertions from TestParser, and expects fragment.GCLogAnalyzer
to produce the same result as normal GCLogAnalyzer.
Be advised that assertions about GCEvent.getPhases are excluded, since fragment.GCLogAnalyzer
doesn't guarantee the same order.
Also, gclog of jdk11 and 17 and 8 with serialGC are excluded for the time being.
 */

@Slf4j
public class TestFragmentedParserToGCModelLegacy {
    public static final double DELTA = 1e-6;

    private GCModel parse(List<String> context) {
        return new GCLogAnalyzer().parseToGCModel(context, new HashMap<String, String>());
    }

    @Test
    public void testJDK11CMSGCParser() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("11CMSGCParser.log");
        Assertions.assertEquals(log.size(), 38);

        CMSGCModel model = (CMSGCModel) parse(log);
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

        GCEvent fullGC = model.getGcEvents().get(2);
        Assertions.assertEquals(fullGC.getGcid(), 2);
        Assertions.assertEquals(fullGC.getStartTime(), 8970, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 178.617, DELTA);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getCause(), ALLOCATION_FAILURE);
        Assertions.assertEquals(fullGC.getMemoryItem(HEAP), new GCMemoryItem(HEAP, 174 * 1024 * 1024, 166 * 1024 * 1024, 247 * 1024 * 1024));
        Assertions.assertEquals(fullGC.getCpuTime().getReal(), 180, DELTA);
        Assertions.assertEquals(fullGC.getPhases().size(), 4);
    }

    @Test
    public void testJDK11G1Parser() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("11G1Parser.log");
        Assertions.assertEquals(log.size(), 64);

        G1GCModel model = (G1GCModel) parse(log);

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
        Assertions.assertEquals(youngGC.getMemoryItem(SURVIVOR)
                , new GCMemoryItem(SURVIVOR, 0, 3 * 1024 * 1024, 3 * 1024 * 1024));
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

        GCEvent fullGC = event.get(2);
        Assertions.assertEquals(fullGC.getGcid(), 2);
        Assertions.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assertions.assertEquals(fullGC.getStartTime(), 7.055 * 1000, DELTA);
        Assertions.assertEquals(fullGC.getPause(), 67.806, DELTA);
        Assertions.assertEquals(fullGC.getDuration(), 67.806, DELTA);
        Assertions.assertEquals(fullGC.getCause(), G1_EVACUATION_PAUSE);
        Assertions.assertEquals(fullGC.getPhases().size(), 4);

        GCLogMetadata metadata = model.getGcModelMetadata();
        Assertions.assertTrue(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS.getName()));
        Assertions.assertFalse(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_UNDO_CYCLE.getName()));
    }

    @Test
    public void testJDK8CMSParser() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8CMSParser.log");
        Assertions.assertEquals(log.size(), 30);

        CMSGCModel model = (CMSGCModel) parse(log);

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
        List<String> log = TestUtil.generateShuffledGCLog("8CMSCPUTime.log");
        Assertions.assertEquals(log.size(), 12);

        CMSGCModel model = (CMSGCModel) parse(log);

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
    public void TestJDK8CMSPromotionFailed() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8CMSPromotionFailed.log");
        Assertions.assertEquals(log.size(), 1);

        CMSGCModel model = (CMSGCModel) parse(log);

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
        List<String> log = TestUtil.generateShuffledGCLog("8CMSScavengeBeforeRemark.log");
        Assertions.assertEquals(log.size(), 14);

        CMSGCModel model = (CMSGCModel) parse(log);

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
    public void testJDK8CMSPrintGC() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8CMSPrintGC.log");
        Assertions.assertEquals(log.size(), 14);

        CMSGCModel model = (CMSGCModel) parse(log);

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

    @Test
    public void testJDK8G1GCParser() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8G1GCParser.log");
        Assertions.assertEquals(log.size(), 1);

        G1GCModel model = (G1GCModel) parse(log);

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
        List<String> log = TestUtil.generateShuffledGCLog("8G1GCParserAdaptiveSize.log");
        Assertions.assertEquals(log.size(), 1);


        G1GCModel model = (G1GCModel) parse(log);

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
    public void testJDK8G1PrintGC() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8G1PrintGC.log");
        Assertions.assertEquals(log.size(), 22);

        G1GCModel model = (G1GCModel) parse(log);

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
    public void testJDK8ParallelGCParser() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8ParallelGCParser.log");
        Assertions.assertEquals(log.size(), 6);

        ParallelGCModel model = (ParallelGCModel) parse(log);

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
    public void testJDK8GenerationalGCInterleave() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8GenerationalGCInterleave.log");
        Assertions.assertEquals(log.size(), 1);

        GCModel model = parse(log);

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
    public void testJDK8ConcurrentPrintDateTimeStamp() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8ConcurrentPrintDateTimeStamp.log");
        Assertions.assertEquals(log.size(), 7);

        G1GCModel model = (G1GCModel) parse(log);

        Assertions.assertEquals(model.getGcEvents().size(), 3);
        Assertions.assertEquals(model.getGcEvents().get(0).getEventType(), GCEventType.YOUNG_GC);
        Assertions.assertEquals(model.getGcEvents().get(1).getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assertions.assertEquals(model.getGcEvents().get(1).getPhases().get(0).getEventType(), GCEventType.G1_CONCURRENT_SCAN_ROOT_REGIONS);
        Assertions.assertEquals(model.getGcEvents().get(1).getPhases().get(0).getStartTime(), 725081, DELTA);
        Assertions.assertEquals(model.getGcEvents().get(2).getEventType(), GCEventType.YOUNG_GC);
    }

    @Test
    public void TestIncompleteGCLog() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("IncompleteGCLog.log");
        Assertions.assertEquals(log.size(), 30);

        CMSGCModel model = (CMSGCModel) parse(log);

        Assertions.assertNotNull(model);

        Assertions.assertEquals(model.getGcEvents().size(), 2);
        Assertions.assertEquals(model.getAllEvents().size(), 8);
    }
}
