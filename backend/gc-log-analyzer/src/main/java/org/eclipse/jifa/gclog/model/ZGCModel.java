/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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

import org.eclipse.jifa.gclog.vo.GCCollectorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.eclipse.jifa.gclog.model.GCEvent.UNKNOWN_INT;
import static org.eclipse.jifa.gclog.model.GCEventType.*;

public class ZGCModel extends GCModel {

    // key of maps here should include unit like
    // "Memory: Allocation Rate MB/s" to deduplicate
    private List<Map<String, ZStatistics>> statistics = new ArrayList<>();
    private List<GCEvent> allocationStalls = new ArrayList<>();
    private int recommendMaxHeapSize = UNKNOWN_INT;

    private final static List<GCEventType> SUPPORTED_PHASE_EVENT_TYPES = Arrays.asList(
            ZGC_GARBAGE_COLLECTION,
            ZGC_PAUSE_MARK_START,
            ZGC_PAUSE_MARK_END,
            ZGC_PAUSE_RELOCATE_START,
            ZGC_CONCURRENT_MARK,
            ZGC_CONCURRENT_NONREF,
            ZGC_CONCURRENT_RESET_RELOC_SET,
            ZGC_CONCURRENT_DETATCHED_PAGES,
            ZGC_CONCURRENT_SELECT_RELOC_SET,
            ZGC_CONCURRENT_PREPARE_RELOC_SET,
            ZGC_CONCURRENT_RELOCATE
    );

    @Override
    protected List<GCEventType> getSupportedPhaseEventTypes() {
        return SUPPORTED_PHASE_EVENT_TYPES;
    }

    private final static List<String> PAUSE_EVENT_NAMES = Arrays.asList(
            ZGC_PAUSE_MARK_START.getName(),
            ZGC_PAUSE_MARK_END.getName(),
            ZGC_PAUSE_RELOCATE_START.getName()
    );

    @Override
    protected List<String> getPauseEventNames() {
        return PAUSE_EVENT_NAMES;
    }

    public ZGCModel() {
        super(GCCollectorType.ZGC);
    }

    private final static List<String> METADATA_EVENT_TYPES = Arrays.asList(
            ZGC_GARBAGE_COLLECTION.getName()
    );

    public List<GCEvent> getAllocationStalls() {
        return allocationStalls;
    }

    public void addAllocationStalls(GCEvent allocationStall) {
        this.allocationStalls.add(allocationStall);
    }

    @Override
    protected List<String> getMetadataEventTypes() {
        return METADATA_EVENT_TYPES;
    }

    public List<Map<String, ZStatistics>> getStatistics() {
        return statistics;
    }

    @Override
    protected boolean isGenerational() {
        return false;
    }

    @Override
    protected boolean isPauseless() {
        return true;
    }

    @Override
    public int getRecommendMaxHeapSize() {
        if (recommendMaxHeapSize == UNKNOWN_INT && !statistics.isEmpty()) {
            // used at marking start + garbage collection cycle * allocation rate
            int statisticIndex = 0;
            for (GCEvent collection : getGcEvents()) {
                if (collection.getEventType() != ZGC_GARBAGE_COLLECTION) {
                    continue;
                }
                if (collection.getCollectionResult() == null ||
                        collection.getCollectionResult().getSummary() == null ||
                        collection.getCollectionResult().getSummary().getPreUsed() == UNKNOWN_INT) {
                    continue;
                }
                while (statisticIndex < statistics.size() &&
                        statistics.get(statisticIndex).get("Collector: Garbage Collection Cycle ms").getUptime() < collection.getEndTime()) {
                    statisticIndex++;
                }
                if (statisticIndex >= statistics.size()) {
                    break;
                }
                double collectionCycleMs = statistics.get(statisticIndex).get("Collector: Garbage Collection Cycle ms").getMax10s();
                double allocationRateMBps = statistics.get(statisticIndex).get("Memory: Allocation Rate MB/s").getMax10s();
                double size = collection.getCollectionResult().getSummary().getPreUsed() +
                        (collectionCycleMs / MS2S) * (allocationRateMBps * KB2MB);
                recommendMaxHeapSize = Math.max(recommendMaxHeapSize, (int) size);
            }
        }
        return recommendMaxHeapSize;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZStatistics {
        private double uptime;
        private double avg10s;
        private double max10s;
        private double avg10m;
        private double max10m;
        private double avg10h;
        private double max10h;
        private double avgTotal;
        private double maxTotal;
    }
}
