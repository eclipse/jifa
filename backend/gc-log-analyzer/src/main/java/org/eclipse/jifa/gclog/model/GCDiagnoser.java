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

package org.eclipse.jifa.gclog.model;

import org.eclipse.jifa.gclog.model.GCModel.KPIItem;
import org.eclipse.jifa.gclog.vo.GCCollectorType;
import org.eclipse.jifa.gclog.vo.GCLogStyle;
import org.eclipse.jifa.gclog.vo.GCSpecialSituation;
import org.eclipse.jifa.gclog.vo.HeapGeneration;

import java.util.*;

import static org.eclipse.jifa.gclog.model.GCEvent.UNKNOWN_INT;
import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.model.GCModel.KB2MB;
import static org.eclipse.jifa.gclog.model.GCModel.KPIType.*;

public class GCDiagnoser {
    private GCModel model;
    private List<GCModel.ProblemAndSuggestion> diagnosis = new ArrayList<>();

    public GCDiagnoser(GCModel model) {
        this.model = model;
    }

    public List<GCModel.ProblemAndSuggestion> diagnose() {
        collector();
        kpi();
        cause();
        g1Humongous();
        g1FrequentConcurrentCycle();
        g1YoungSuddenlyBecomesSmall();
        cmsFrequentConcurrentMark();
        specialSituation();
        eventType();
        generationSize();
        allocationStall();
        return diagnosis;
    }

    private void collector() {
        if (model.getCollectorType() == GCCollectorType.CMS && model.getLogStyle() == GCLogStyle.UNIFIED_STYLE) {
            addDiagnose(new GCModel.ProblemAndSuggestion(
                    new I18nStringView("jifa.gclog.diagnosis.problems.jdk11cms"),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.dontUseCMSAnyMore")
            ));
        }
    }

    private static final int G1_YOUNG_BECOME_SMALL_THRESHOLD = 3;

    private void g1YoungSuddenlyBecomesSmall() {
        if (model.getCollectorType() != GCCollectorType.G1) {
            return;
        }
        int lastYoungSize = 0;
        int lastHeapSize = 0;
        for (GCEvent event : model.getGcEvents()) {
            if (event.getCollectionAgg() == null && event.getEventType() != FULL_GC) {
                continue;
            }
            int youngSize = event.getCollectionAgg().get(HeapGeneration.YOUNG).getTotal();
            int heapSize = event.getCollectionAgg().get(HeapGeneration.TOTAL).getTotal();
            if (youngSize <= 0 || heapSize <= 0) {
                continue;
            }
            if (lastHeapSize == heapSize && lastYoungSize / youngSize >= G1_YOUNG_BECOME_SMALL_THRESHOLD) {
                addDiagnose(new GCModel.ProblemAndSuggestion(
                        new I18nStringView("jifa.gclog.diagnosis.problems.g1YoungSuddenlyBecomesSmall",
                                "time", event.getStartTimeString()),
                        new I18nStringView("jifa.gclog.diagnosis.suggestions.longPauseToShrinkYoung"),
                        new I18nStringView("jifa.gclog.diagnosis.suggestions.updateJDK")
                ));
                return;
            }
            lastYoungSize = youngSize;
            lastHeapSize = heapSize;
        }
    }

    private void kpi() {
        throughput();
        zgcTooFrequent();
    }

    private static final double ZGC_FREQUENT_THRESHOLD = 0.75;

    private void zgcTooFrequent() {
        if (model.getCollectorType() != GCCollectorType.ZGC) {
            return;
        }
        KPIItem item = model.getKpi().get(GC_DURATION_PERCENTAGE.getName());
        double percent = item.getValue();
        if (percent > ZGC_FREQUENT_THRESHOLD) {
            addDiagnose(zgcSuggestHeapAndThread(
                    new I18nStringView("jifa.gclog.diagnosis.problems.zgcTooFrequent", "percent", String.format("%.2f", 100 * percent))));
            item.setBad(true);
        }
    }

    private static final double BAD_ALLOCATION_STALL_OPS_S = 1000;
    private static final double BAD_ALLOCATION_STALL_MS = 1000.0;

    private void allocationStall() {
        if (model.getCollectorType() != GCCollectorType.ZGC) {
            return;
        }
        ZGCModel zgcModel = (ZGCModel) model;
        if (zgcModel.getStatistics().isEmpty()) {
            return;
        }
        Map<String, ZGCModel.ZStatistics> lastStatistic = zgcModel.getStatistics().get(zgcModel.getStatistics().size() - 1);
        if (lastStatistic.get("Critical: Allocation Stall ms").getMaxTotal() > BAD_ALLOCATION_STALL_MS &&
                lastStatistic.get("Critical: Allocation Stall ops/s").getMaxTotal() > BAD_ALLOCATION_STALL_OPS_S) {
            GCModel.ProblemAndSuggestion pas =
                    zgcSuggestHeapAndThread(new I18nStringView("jifa.gclog.diagnosis.problems.allocationStall"));
            pas.addSuggestion(new I18nStringView("jifa.gclog.diagnosis.suggestions.addZGCAllocationSpikeTolerance"));
            addDiagnose(pas);
        }
    }

    private static final double ZGC_HEAP_CHANGE_TOO_SMALL_THRESHOLD = 0.1;

    private GCModel.ProblemAndSuggestion zgcSuggestHeapAndThread(I18nStringView problem) {
        assert model instanceof ZGCModel;
        GCModel.ProblemAndSuggestion pas = new GCModel.ProblemAndSuggestion();
        pas.setProblem(problem);
        pas.addSuggestion(new I18nStringView("jifa.gclog.diagnosis.suggestions.addConcurrentGCThread"));
        int recommendSize = model.getRecommendMaxHeapSize();
        int actualSize = model.basicInfo.getHeapSize();
        if (recommendSize != UNKNOWN_INT && recommendSize > actualSize &&
                (recommendSize - actualSize) / (double) recommendSize > ZGC_HEAP_CHANGE_TOO_SMALL_THRESHOLD) {
            pas.addSuggestion(new I18nStringView("jifa.gclog.diagnosis.suggestions.enlargeHeapWithRecommend",
                    "size", (int) (recommendSize / KB2MB / KB2MB)));
        } else {
            pas.addSuggestion(new I18nStringView("jifa.gclog.diagnosis.suggestions.enlargeHeap"));
        }
        return pas;
    }

    public static final double SMALL_GENERATION_THRESHOLD = 0.1;

    private void generationSize() {
        if (model.basicInfo.getYoungGenSize() == UNKNOWN_INT || model.basicInfo.getOldGenSize() == UNKNOWN_INT
                || model.basicInfo.getHeapSize() == UNKNOWN_INT) {
            return;
        }
        I18nStringView problem = null;
        I18nStringView suggestion;
        double percent = (double) model.basicInfo.getYoungGenSize() / model.basicInfo.getHeapSize();
        if (percent < SMALL_GENERATION_THRESHOLD) {
            problem = new I18nStringView("jifa.gclog.diagnosis.problems.smallYoungGen",
                    "percent", String.format("%.2f", 100 * percent));
        }
        percent = (double) model.basicInfo.getOldGenSize() / model.basicInfo.getHeapSize();
        if (percent < SMALL_GENERATION_THRESHOLD) {
            problem = new I18nStringView("jifa.gclog.diagnosis.problems.smallOldGen",
                    "percent", String.format("%.2f", 100 * percent));
        }
        if (problem != null) {
            suggestion = new I18nStringView("jifa.gclog.diagnosis.suggestions.setReasonableXmn");
            addDiagnose(new GCModel.ProblemAndSuggestion(problem, suggestion));
        }
    }

    private void cmsFrequentConcurrentMark() {
        if (model.getCollectorType() != GCCollectorType.CMS) {
            return;
        }
        List<GCEvent> gcEvents = model.getGcEvents();
        for (int i = 0; i < gcEvents.size() - 2; i++) {
            if (gcEvents.get(i).getEventType() == CMS_CONCURRENT_MARK_SWEPT &&
                    gcEvents.get(i + 1).getEventType() == YOUNG_GC &&
                    gcEvents.get(i + 2).getEventType() == CMS_CONCURRENT_MARK_SWEPT) {
                addDiagnose(new GCModel.ProblemAndSuggestion(
                        new I18nStringView("jifa.gclog.diagnosis.problems.cmsFrequentConcurrentMarkSwept"),
                        new I18nStringView("jifa.gclog.diagnosis.suggestions.enlargeOld"),
                        new I18nStringView("jifa.gclog.diagnosis.suggestions.reviewOld")
                ));
                return;
            }
        }
    }

    private void g1FrequentConcurrentCycle() {
        if (model.getCollectorType() != GCCollectorType.G1) {
            return;
        }
        List<GCEvent> gcEvents = model.getGcEvents();
        for (int i = 0; i < gcEvents.size() - 1; i++) {
            if (gcEvents.get(i).getEventType() == G1_YOUNG_MIXED_GC &&
                    gcEvents.get(i + 1).hasSpecialSituation(GCSpecialSituation.INITIAL_MARK)) {
                addDiagnose(new GCModel.ProblemAndSuggestion(
                        new I18nStringView("jifa.gclog.diagnosis.problems.g1FrequentConcurrentCycle"),
                        new I18nStringView("jifa.gclog.diagnosis.suggestions.reviewOld")
                ));
                return;
            }
        }
    }

    private static final String G1_HUMONGOUS_ALLOCATION = "G1 Humongous Allocation";
    private static final int HUMONGOUS_TO_SPACE_THRESHOLD = 10;

    private void g1Humongous() {
        if (model.getCollectorType() != GCCollectorType.G1) {
            return;
        }
        List<GCEvent> gcEvents = model.getGcEvents();
        for (int i = 0; i < gcEvents.size() - HUMONGOUS_TO_SPACE_THRESHOLD - 1; i++) {
            GCEvent gcEvent = gcEvents.get(i);
            if (G1_HUMONGOUS_ALLOCATION.equals(gcEvent.getCause())) {
                for (int j = 1; j <= HUMONGOUS_TO_SPACE_THRESHOLD; j++) {
                    if (gcEvents.get(i + j).hasSpecialSituation(GCSpecialSituation.TO_SPACE_EXHAUSTED)) {
                        GCModel.ProblemAndSuggestion diagnose = new GCModel.ProblemAndSuggestion(
                                new I18nStringView("jifa.gclog.diagnosis.problems.humongousToSpaceExhausted"),
                                new I18nStringView("jifa.gclog.diagnosis.suggestions.reviewHuge"),
                                new I18nStringView("jifa.gclog.diagnosis.suggestions.addHeapRegionSize")
                        );
                        suggestUpdateToJDK11G1(diagnose);
                        addDiagnose(diagnose);
                        return;
                    }
                }
            }
        }
    }

    private final static double LOW_THROUGHPUT_THRESHOLD = 0.9;

    private void throughput() {
        if (model.getCollectorType() == GCCollectorType.ZGC) {
            return;
        }
        KPIItem item = model.getKpi().get(THROUGHPUT.getName());
        double throughput = item.getValue();
        if (throughput >= 0 && throughput < LOW_THROUGHPUT_THRESHOLD) {
            item.setBad(true);
            GCModel.ProblemAndSuggestion problemAndSuggestion = new GCModel.ProblemAndSuggestion();
            problemAndSuggestion.setProblem(new I18nStringView("jifa.gclog.diagnosis.problems.lowThroughput", "throughput", String.format("%.2f", 100 * throughput)));
            problemAndSuggestion.addSuggestion(new I18nStringView("jifa.gclog.diagnosis.suggestions.addGCThread"));
            problemAndSuggestion.addSuggestion(new I18nStringView("jifa.gclog.diagnosis.suggestions.enlargeHeap"));
            if (model.getCollectorType() != GCCollectorType.G1) {
                problemAndSuggestion.addSuggestion(new I18nStringView("jifa.gclog.diagnosis.suggestions.enlargeYoung"));
            }
            addDiagnose(problemAndSuggestion);
        }
    }

    private final static String METASPACE_GC_THRESHOLD = "Metadata GC Threshold";
    private final static String SYSTEM_GC = "System.gc()";

    private void cause() {
        CountingMap<String> causes = new CountingMap<>();
        for (GCEvent event : model.getGcEvents()) {
            if (event.getCause() == null) {
                continue;
            }
            if (event.getCause().equals(METASPACE_GC_THRESHOLD) && event.getEventType() != FULL_GC) {
                continue;
            }
            if (event.getCause().equals(SYSTEM_GC) && event.getEventType() != FULL_GC) {
                continue;
            }
            causes.put(event.getCause());
        }

        if (causes.containKey(METASPACE_GC_THRESHOLD)) {
            addDiagnose(new GCModel.ProblemAndSuggestion(
                    new I18nStringView("jifa.gclog.diagnosis.problems.metaspaceFullGC", "count", causes.get(METASPACE_GC_THRESHOLD)),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.enlargeMetaspace"),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.reviewMetaspace")
            ));
        }

        if (causes.containKey(SYSTEM_GC)) {
            addDiagnose(new GCModel.ProblemAndSuggestion(
                    new I18nStringView("jifa.gclog.diagnosis.problems.systemGC", "count", causes.get(SYSTEM_GC)),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.reviewSystemGC"),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.disableSystemGC")
            ));
        }

        if (causes.containKey(GCSpecialSituation.PROMOTION_FAILED.getName())) {
            addDiagnose(new GCModel.ProblemAndSuggestion(
                    new I18nStringView("jifa.gclog.diagnosis.problems.promotionFailed", "count",
                            causes.get(GCSpecialSituation.PROMOTION_FAILED.getName())),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.enlargeOld"),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.reviewOld")
            ));
        }
    }

    private void specialSituation() {
        CountingMap<GCSpecialSituation> specialCount = new CountingMap<>();
        for (GCEvent gcEvent : model.getGcEvents()) {
            if (gcEvent.getSpecialSituations() != null) {
                for (GCSpecialSituation specialSituation : gcEvent.getSpecialSituations()) {
                    specialCount.put(specialSituation);
                }
            }
        }

        if (model.getCollectorType() == GCCollectorType.G1 &&
                specialCount.containKey(GCSpecialSituation.TO_SPACE_EXHAUSTED)) {
            GCModel.ProblemAndSuggestion diagnose = new GCModel.ProblemAndSuggestion();
            diagnose.setProblem(new I18nStringView("jifa.gclog.diagnosis.problems.g1ToSpaceExhausted", "count", specialCount.get(GCSpecialSituation.TO_SPACE_EXHAUSTED)));
            VmOptions vmOptions = model.getVmOptions();
            if (vmOptions == null || vmOptions.containOption("Xmn")) {
                diagnose.addSuggestion(new I18nStringView("jifa.gclog.diagnosis.suggestions.noXMN"));
            }
            if (vmOptions != null && vmOptions.containOption("MaxNewSize") && vmOptions.containOption("NewSize") &&
                    vmOptions.getOptionValue("MaxNewSize").equals(vmOptions.getOptionValue("NewSize"))) {
                diagnose.addSuggestion(new I18nStringView("jifa.gclog.diagnosis.suggestions.noNewSizeEqual"));
            }
            addDiagnose(diagnose);
        }
    }

    void eventType() {
        CountingMap<GCEventType> counts = new CountingMap<>();
        for (GCEvent gcEvent : model.getGcEvents()) {
            counts.put(gcEvent.getEventType());
            if (gcEvent.hasPhases()) {
                for (GCEvent phase : gcEvent.getPhases()) {
                    counts.put(phase.getEventType());
                }
            }
        }

        if (counts.containKey(FULL_GC)) {
            GCModel.ProblemAndSuggestion diagnose = new GCModel.ProblemAndSuggestion(
                    new I18nStringView("jifa.gclog.diagnosis.problems.fullGC", "count", counts.get(FULL_GC)),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.reviewFullGCCause"),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.reviewOld")
            );
            suggestUpdateToJDK11G1(diagnose);
            addDiagnose(diagnose);
        }

        if (model.getCollectorType() == GCCollectorType.CMS &&
                counts.containKey(GCEventType.CMS_CONCURRENT_FAILURE)) {
            addDiagnose(new GCModel.ProblemAndSuggestion(
                    new I18nStringView("jifa.gclog.diagnosis.problems.concurrentFailure", "count", counts.get(GCEventType.CMS_CONCURRENT_FAILURE)),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.enlargeOld"),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.lowerCMSOldGCThreshold"),
                    new I18nStringView("jifa.gclog.diagnosis.suggestions.reviewOld")
            ));
        }
    }

    private void addDiagnose(GCModel.ProblemAndSuggestion diagnose) {
        if (diagnose != null) {
            diagnosis.add(diagnose);
        }
    }

    private void suggestUpdateToJDK11G1(GCModel.ProblemAndSuggestion diagnose) {
        if (model.getCollectorType() == GCCollectorType.G1) {
            diagnose.addSuggestion(new I18nStringView("jifa.gclog.diagnosis.suggestions.updateToJDK11G1"));
        }
    }

    private static class CountingMap<T> {
        private Map<T, Integer> map = new HashMap<>();

        public void put(T key) {
            map.put(key, map.getOrDefault(key, 0) + 1);
        }

        public boolean containKey(T key) {
            return map.containsKey(key);
        }

        public int get(T key) {
            return map.getOrDefault(key, 0);
        }
    }
}
