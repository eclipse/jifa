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
import org.eclipse.jifa.gclog.model.*;
import org.eclipse.jifa.gclog.vo.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.eclipse.jifa.gclog.model.GCEvent.*;
import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.vo.HeapGeneration.*;

public class TestGCModel {

    public static final double DELTA = 1e-6;
    G1GCModel model;

    @Before
    public void mockModel() {
        GCCollectionResult collectionResult;
        model = new G1GCModel();
        model.setHeapRegionSize(1024 * 1024);
        model.setCollectorType(GCCollectorType.G1);
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
        collectionResult = new GCCollectionResult();
        collectionResult.addItem(new GCCollectionResultItem(EDEN, 20 * 1024 * 1024, 0, 100 * 1024 * 1024));
        collectionResult.addItem(new GCCollectionResultItem(SURVIVOR, 0, 10 * 1024 * 1024, 100 * 1024 * 1024));
        collectionResult.addItem(new GCCollectionResultItem(HUMONGOUS, 10 * 1024 * 1024, 10 * 1024 * 1024, UNKNOWN_INT));
        collectionResult.addItem(new GCCollectionResultItem(METASPACE, 15 * 1024 * 1024, 15 * 1024 * 1024, 20 * 1024 * 1024));
        collectionResult.addItem(new GCCollectionResultItem(OLD, 10 * 1024 * 1024, 12 * 1024 * 1024, 100 * 1024 * 1024));
        events[0].addSpecialSituation(GCSpecialSituation.TO_SPACE_EXHAUSTED);
//      expected result:  collectionResult.addItem(new GCCollectionResultItem(TOTAL,40*1024,32*1024,300*1024));
        events[0].setCollectionResult(collectionResult);

        events[1].setEventType(GCEventType.FULL_GC);
        events[1].setStartTime(3.0 * 1000);
        events[1].setDuration(0.4 * 1000);
        events[1].setCause("G1 Evacuation Pause");
        collectionResult = new GCCollectionResult();
        collectionResult.addItem(new GCCollectionResultItem(EDEN, 30 * 1024 * 1024, 0, 100 * 1024 * 1024));
        collectionResult.addItem(new GCCollectionResultItem(SURVIVOR, 0, 0, 100 * 1024 * 1024));
        collectionResult.addItem(new GCCollectionResultItem(HUMONGOUS, 10 * 1024 * 1024, 10 * 1024 * 1024, UNKNOWN_INT));
        collectionResult.addItem(new GCCollectionResultItem(OLD, 10 * 1024 * 1024, 30 * 1024 * 1024, 100 * 1024 * 1024));
//      expected result:  collectionResult.addItem(new GCCollectionResultItem(TOTAL,50,40,300));
        events[1].setCollectionResult(collectionResult);

        events[2].setEventType(FULL_GC);
        events[2].setStartTime(12.0 * 1000);
        events[2].setDuration(1.0 * 1000);
        events[2].setCause("Metadata GC Threshold");
        collectionResult = new GCCollectionResult();
        collectionResult.addItem(new GCCollectionResultItem(EDEN, 24 * 1024 * 1024, 0, 100 * 1024 * 1024));
        collectionResult.addItem(new GCCollectionResultItem(SURVIVOR, 8 * 1024 * 1024, 0, 100 * 1024 * 1024));
        collectionResult.addItem(new GCCollectionResultItem(HUMONGOUS, 20 * 1024 * 1024, 20 * 1024 * 1024, UNKNOWN_INT));
        collectionResult.addItem(new GCCollectionResultItem(OLD, 10 * 1024 * 1024, 10 * 1024 * 1024, 100 * 1024 * 1024));
        collectionResult.addItem(new GCCollectionResultItem(TOTAL, 62 * 1024 * 1024, 30 * 1024 * 1024, 300 * 1024 * 1024));
        events[2].setCollectionResult(collectionResult);

        events[3].setEventType(G1_CONCURRENT_CYCLE);
        events[3].setStartTime(16.0 * 1000);
        events[3].setDuration(1.2 * 1000);
        GCEventType[] concurrentCyclePhases = {REMARK, G1_PAUSE_CLEANUP, G1_CONCURRENT_CLEAR_CLAIMED_MARKS};
        double[] begins = {12.0, 12.2, 12.6};
        for (int i = 0; i < concurrentCyclePhases.length; i++) {
            GCEvent phase = new GCEvent(4);
            model.addPhase(events[3], phase);
            phase.setEventType(concurrentCyclePhases[i]);
            phase.setDuration(0.2 * (i + 1) * 1000);
            phase.setStartTime(begins[i] * 1000);
        }
        events[3].getPhases().get(0).setCollectionResult(new GCCollectionResult(new GCCollectionResultItem(TOTAL, 70 * 1024 * 1024, 70 * 1024 * 1024, 300 * 1024 * 1024)));
        events[3].getPhases().get(1).setCollectionResult(new GCCollectionResult(new GCCollectionResultItem(TOTAL, 70 * 1024 * 1024, 70 * 1024 * 1024, 300 * 1024 * 1024)));

        events[4].setEventType(G1_CONCURRENT_CYCLE);
        events[4].setStartTime(24 * 1000);
        events[4].setDuration(0.6 * 1000);
        concurrentCyclePhases = new GCEventType[]{REMARK, G1_PAUSE_CLEANUP, G1_CONCURRENT_CLEAR_CLAIMED_MARKS};
        begins = new double[]{24.0, 24.1, 24.3};
        for (int i = 0; i < concurrentCyclePhases.length; i++) {
            GCEvent phase = new GCEvent(5);
            model.addPhase(events[4], phase);
            phase.setEventType(concurrentCyclePhases[i]);
            phase.setDuration(0.1 * (i + 1) * 1000);
            phase.setStartTime(begins[i] * 1000);
        }
        events[4].getPhases().get(0).setCollectionResult(new GCCollectionResult(new GCCollectionResultItem(TOTAL, 70  * 1024 * 1024, 70  * 1024 * 1024, 300  * 1024 * 1024)));
        events[4].getPhases().get(1).setCollectionResult(new GCCollectionResult(new GCCollectionResultItem(TOTAL, 70  * 1024 * 1024, 70  * 1024 * 1024, 300  * 1024 * 1024)));

        events[5].setEventType(YOUNG_GC);
        events[5].setStartTime(32.0 * 1000);
        events[5].setDuration(0.3 * 1000);
        events[5].setCause("G1 Evacuation Pause");
        collectionResult = new GCCollectionResult();
        collectionResult.addItem(new GCCollectionResultItem(EDEN, 16  * 1024 * 1024, 0, 100  * 1024 * 1024));
        collectionResult.addItem(new GCCollectionResultItem(SURVIVOR, 0, 4  * 1024 * 1024, 100  * 1024 * 1024));
        collectionResult.addItem(new GCCollectionResultItem(HUMONGOUS, 50  * 1024 * 1024, 50  * 1024 * 1024, UNKNOWN_INT));
        collectionResult.addItem(new GCCollectionResultItem(METASPACE, 15  * 1024 * 1024, 15  * 1024 * 1024, 40  * 1024 * 1024));
//      expected result:  collectionResult.addItem(new GCCollectionResultItem(OLD,10,12,100));
        collectionResult.addItem(new GCCollectionResultItem(TOTAL, 76  * 1024 * 1024, 66  * 1024 * 1024, 300  * 1024 * 1024));
        events[5].setCollectionResult(collectionResult);

        Safepoint safepoint = new Safepoint();
        safepoint.setStartTime(31.9 * 1000);
        safepoint.setTimeToEnter(0.6 * 1000);
        safepoint.setDuration(0.3 * 1000);
        model.putEvent(safepoint);
    }

    @Test
    public void testCalculateDerivedInfo() {
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
        Assert.assertEquals(model.getGcEvents().get(0).getCollectionAgg().get(TOTAL),
                new GCCollectionResultItem(TOTAL, 40 * 1024 * 1024, 32 * 1024 * 1024, 300 * 1024 * 1024));
        Assert.assertEquals(model.getGcEvents().get(6).getCollectionAgg().get(OLD),
                new GCCollectionResultItem(OLD, 10 * 1024 * 1024, 12 * 1024 * 1024, 100 * 1024 * 1024));
        Assert.assertEquals(model.getGcEvents().get(0).getStartTimestamp(), 2.0 * 1000, DELTA);
        Assert.assertEquals(model.getGcEvents().get(6).getEndTimestamp(), 33.3 * 1000, DELTA);
        Assert.assertEquals(model.getGcEvents().get(4).getInterval(), 6.8 * 1000, DELTA);

        // object statistics
        ObjectStatistics objectStatistics = model.getObjectStatistics(new TimeRange(500, 33000));
        Assert.assertEquals(objectStatistics.getObjectCreationSpeed(), (40 + 18 + 22 + 40 + 6) * 1024 * 1024 / 31800.0, DELTA);
        Assert.assertEquals(objectStatistics.getObjectPromotionSpeed(), (2 + 2) * 1024 * 1024 / 31800.0, DELTA);
        Assert.assertEquals(objectStatistics.getObjectPromotionAvg(), 2 * 1024 * 1024 );
        Assert.assertEquals(objectStatistics.getObjectPromotionMax(), 2 * 1024 * 1024);


        // graph data
        TimeLineChartView count = model.getGraphView("count", 300000, 0);
        Assert.assertEquals(count.getDataByTimes().get(0).getData().get(1000L), 1, DELTA);
        Assert.assertEquals(count.getDataByTimes().get(3).getData().get(3000L), 1, DELTA);

        TimeLineChartView pause = model.getGraphView("pause", 300000, 0);
        Assert.assertEquals(pause.getDataByTimes().get(0).getData().get(1500L), 500, DELTA);
        Assert.assertEquals(pause.getDataByTimes().get(3).getData().get(12200L), 200, DELTA);

        TimeLineChartView alloRec = model.getGraphView("alloRec", 10800000, 0);
        Assert.assertEquals(alloRec.getDataByTimes().get(0).getData().get(0L), (40 + 40 + 18 + 22 + 6) / 60.0, DELTA);
        Assert.assertEquals(alloRec.getDataByTimes().get(1).getData().get(0L), (8 + 10 + 32 + 10) / 60.0, DELTA);

        TimeLineChartView promotion = model.getGraphView("promotion", 10800000, 0);
        Assert.assertEquals(promotion.getDataByTimes().get(0).getData().get(0L), (2 + 2) * 1024 / 60.0, DELTA);

        TimeLineChartView heap = model.getGraphView("heap", 10800000, 0);
        Assert.assertEquals(heap.getDataByTimes().get(0).getData().get(1000L), 20 * 1024, DELTA);
        Assert.assertEquals(heap.getDataByTimes().get(0).getData().get(1500L), 10 * 1024, DELTA);
        Assert.assertEquals(heap.getDataByTimes().get(1).getData().get(1000L), 10 * 1024, DELTA);
        Assert.assertEquals(heap.getDataByTimes().get(1).getData().get(1500L), 12 * 1024, DELTA);

        TimeLineChartView metaspace = model.getGraphView("metaspace", 10800000, 0);
        Assert.assertEquals(metaspace.getDataByTimes().get(0).getData().get(1000L), 15 * 1024, DELTA);
        Assert.assertEquals(metaspace.getDataByTimes().get(0).getData().get(1500L), 15 * 1024, DELTA);
        Assert.assertEquals(metaspace.getDataByTimes().get(1).getData().get(1500L), 20 * 1024, DELTA);

        // phase info
        Assert.assertEquals(model.getGcPhaseInfo().get(0).getName(), "Young GC");
        Assert.assertEquals(model.getGcPhaseInfo().get(0).getCount(), 2);
        Assert.assertEquals(model.getGcPhaseInfo().get(0).getMaxTime(), 0.5 * 1000, DELTA);
        Assert.assertEquals(model.getGcPhaseInfo().get(0).getTotalTime(), (0.3 + 0.5) * 1000, DELTA);
        Assert.assertEquals(model.getGcPhaseInfo().get(0).getAvgTime(), (0.3 + 0.5) / 2 * 1000, DELTA);
        Assert.assertEquals(model.getGcPhaseInfo().get(1).getName(), "Full GC");
        Assert.assertEquals(model.getGcPhaseInfo().get(3).getName(), "Remark");
        Assert.assertEquals(model.getGcPhaseInfo().get(3).getAvgTime(), (0.2 + 0.1) / 2 * 1000, DELTA);

        // diagnose
        Assert.assertTrue(hasDiagnose("jifa.gclog.diagnosis.problems.metaspaceFullGC"));
        Assert.assertTrue(hasDiagnose("jifa.gclog.diagnosis.problems.fullGC"));

        // event toString
        Assert.assertEquals(model.getGcEvents().get(0).toString(),
                "(0)1970-01-01 08:00:02.000 1.000: [Young GC (G1 Evacuation Pause) (To-space Exhausted), 0.500s] [Young: 20M->10M(200M)] [Old: 10M->12M(100M)] [Humongous: 10M->10M] [Total: 40M->32M(300M)] [Metaspace: 15M->15M(20M)] [promotion 2048 K]");
        Assert.assertEquals(model.getGcEvents().get(4).toString(),
                "(4)1970-01-01 08:00:25.000 24.000: [Concurrent Cycle, 0.600s] [interval 6.800s] [Remark 0.100s] [Pause Cleanup 0.200s]");
        Assert.assertEquals(model.getGcEvents().get(5).toString(),
                "1970-01-01 08:00:32.900 31.900: Total time for which application threads were stopped: 300.000 seconds, Stopping threads took: 600.000 seconds");
    }

    boolean hasDiagnose(String name) {
        if (model.getProblemAndSuggestion() == null) {
            return false;
        }
        for (GCModel.ProblemAndSuggestion problemAndSuggestion : model.getProblemAndSuggestion()) {
            if (problemAndSuggestion.getProblem().getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
