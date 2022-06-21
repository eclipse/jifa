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

import org.eclipse.jifa.gclog.vo.GCCollectionResultItem;
import org.eclipse.jifa.gclog.vo.GCCollectorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.eclipse.jifa.gclog.vo.GCLogStyle;
import org.eclipse.jifa.gclog.vo.HeapGeneration;

import java.util.Arrays;
import java.util.List;

import static org.eclipse.jifa.gclog.model.GCEvent.*;
import static org.eclipse.jifa.gclog.model.GCEventType.*;

public class G1GCModel extends GCModel {
    private int heapRegionSize = UNKNOWN_INT;   // in kb
    private boolean regionSizeExact = false;

    public void setRegionSizeExact(boolean regionSizeExact) {
        this.regionSizeExact = regionSizeExact;
    }

    public void setHeapRegionSize(int heapRegionSize) {
        this.heapRegionSize = heapRegionSize;
    }

    public int getHeapRegionSize() {
        return heapRegionSize;
    }

    private final static List<GCEventType> SUPPORTED_PHASE_EVENT_TYPES = Arrays.asList(
            YOUNG_GC,
            FULL_GC,
            INITIAL_MARK,
            G1_CONCURRENT_CYCLE,
            G1_CONCURRENT_SCAN_ROOT_REGIONS,
            CONCURRENT_MARK,
            REMARK,
            G1_PAUSE_CLEANUP
    );

    @Override
    protected List<GCEventType> getSupportedPhaseEventTypes() {
        return SUPPORTED_PHASE_EVENT_TYPES;
    }

    public G1GCModel() {
        super(GCCollectorType.G1);
    }

    private final static List<String> METADATA_EVENT_TYPES = Arrays.asList(
            YOUNG_GC.getName(),
            G1_YOUNG_MIXED_GC.getName(),
            G1_CONCURRENT_CYCLE.getName(),
            FULL_GC.getName()
    );

    @Override
    protected List<String> getMetadataEventTypes() {
        return METADATA_EVENT_TYPES;

    }

    private final static List<String> PAUSE_EVENT_NAMES = Arrays.asList(
            YOUNG_GC.getName(),
            G1_YOUNG_MIXED_GC.getName(),
            FULL_GC.getName(),
            REMARK.getName(),
            G1_PAUSE_CLEANUP.getName()
    );

    @Override
    protected List<String> getPauseEventNames() {
        return PAUSE_EVENT_NAMES;
    }

    private boolean collectionResultUsingRegion(GCEvent event) {
        GCEventType type = event.getEventType();
        return (type == YOUNG_GC || type == FULL_GC || type == G1_YOUNG_MIXED_GC) &&
                event.getCollectionResult() != null && event.getCollectionResult().getItems() != null;
    }

    private void inferHeapRegionSize() {
        if (heapRegionSize != UNKNOWN_INT) {
            return;
        }
        for (int i = getGcEvents().size() - 1; i >= 0; i--) {
            GCEvent event = getGcEvents().get(i);
            if (!collectionResultUsingRegion(event)) {
                continue;
            }
            if (event.getCollectionResult().getSummary().getPreUsed() == UNKNOWN_INT) {
                continue;
            }
            int regionCount = event.getCollectionResult().getItems().stream()
                    .filter(item -> item.getGeneration() != HeapGeneration.METASPACE)
                    .mapToInt(GCCollectionResultItem::getPreUsed)
                    .sum();
            double kbPerRegion = event.getCollectionResult().getSummary().getPreUsed() / (double) regionCount;
            heapRegionSize = (int) Math.pow(2, Math.ceil(Math.log(kbPerRegion) / Math.log(2)));
            return;
        }
    }

    private void adjustMemoryInfo() {
        if (heapRegionSize == UNKNOWN_INT) {
            return;
        }
        for (GCEvent event : getGcEvents()) {
            if (!collectionResultUsingRegion(event)) {
                continue;
            }
            for (GCCollectionResultItem item : event.getCollectionResult().getItems()) {
                if (item.getGeneration() != HeapGeneration.METASPACE) {
                    item.multiply(heapRegionSize);
                }
            }
        }
    }

    @Override
    protected void doBeforeCalculatingDerivedInfo() {
        if (getLogStyle() == GCLogStyle.UNIFIED_STYLE) {
            inferHeapRegionSize();
            adjustMemoryInfo();
        }
    }
}
