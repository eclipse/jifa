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

import com.google.common.util.concurrent.AtomicDouble;
import org.eclipse.jifa.gclog.util.LongData;
import org.eclipse.jifa.gclog.vo.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.jifa.gclog.model.GCEvent.*;
import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.vo.HeapGeneration.*;

public class G1GCModel extends GCModel {
    private long heapRegionSize = UNKNOWN_INT;   // in kb
    private boolean regionSizeExact = false;
    private static GCCollectorType collector = GCCollectorType.G1;

    public void setRegionSizeExact(boolean regionSizeExact) {
        this.regionSizeExact = regionSizeExact;
    }

    public void setHeapRegionSize(long heapRegionSize) {
        this.heapRegionSize = heapRegionSize;
    }

    public long getHeapRegionSize() {
        return heapRegionSize;
    }


    public G1GCModel() {
        super(collector);
    }

    private static List<GCEventType> allEventTypes = GCModel.calcAllEventTypes(collector);
    private static List<GCEventType> pauseEventTypes = GCModel.calcPauseEventTypes(collector);
    private static List<GCEventType> mainPauseEventTypes = GCModel.calcMainPauseEventTypes(collector);
    private static List<GCEventType> parentEventTypes = GCModel.calcParentEventTypes(collector);
    private static List<GCEventType> importantEventTypes = List.of(YOUNG_GC, G1_MIXED_GC, FULL_GC, G1_CONCURRENT_CYCLE,
            G1_CONCURRENT_MARK, G1_REMARK, G1_CONCURRENT_REBUILD_REMEMBERED_SETS, G1_PAUSE_CLEANUP);

    @Override
    protected List<GCEventType> getAllEventTypes() {
        return allEventTypes;
    }

    @Override
    protected List<GCEventType> getPauseEventTypes() {
        return pauseEventTypes;
    }

    @Override
    protected List<GCEventType> getMainPauseEventTypes() {
        return mainPauseEventTypes;
    }

    @Override
    protected List<GCEventType> getImportantEventTypes() {
        return importantEventTypes;
    }

    @Override
    protected List<GCEventType> getParentEventTypes() {
        return parentEventTypes;
    }


    private boolean collectionResultUsingRegion(GCEvent event) {
        GCEventType type = event.getEventType();
        return (type == YOUNG_GC || type == FULL_GC || type == G1_MIXED_GC) &&
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
            long regionCount = event.getCollectionResult().getItems().stream()
                    .filter(item -> item.getGeneration() != HeapGeneration.METASPACE)
                    .mapToLong(GCCollectionResultItem::getPreUsed)
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
        if (getLogStyle() == GCLogStyle.UNIFIED) {
            inferHeapRegionSize();
            adjustMemoryInfo();
        }
    }

    @Override
    protected void calculateUsedAvgAfterOldGC(TimeRange range, LongData[][] data) {
        AtomicReference<GCEvent> lastMixedGC = new AtomicReference<>();
        AtomicDouble lastRemarkEndTime = new AtomicDouble(Double.MAX_VALUE);
        iterateEventsWithinTimeRange(getGcEvents(), range, event -> {
            GCEventType type = event.getEventType();
            // read old from the last mixed gc of old gc cycle
            if (type == G1_MIXED_GC) {
                lastMixedGC.set(event);
            } else if (type == YOUNG_GC || type == G1_CONCURRENT_CYCLE || type == FULL_GC) {
                GCEvent mixedGC = lastMixedGC.get();
                if (mixedGC != null) {
                    if (mixedGC.getCollectionAgg() != null) {
                        data[1][3].add(mixedGC.getCollectionAgg().get(OLD).getPostUsed());
                    }
                    lastMixedGC.set(null);
                }
            }
            // read humongous and metaspace from the gc after remark
            if (event.getEventType() == G1_CONCURRENT_CYCLE) {
                if (event.getLastPhaseOfType(G1_CONCURRENT_MARK_ABORT) != null) {
                    return;
                }
                GCEvent remark = event.getLastPhaseOfType(G1_REMARK);
                lastRemarkEndTime.set(remark.getEndTime());
            } else if ((event.getEventType() == YOUNG_GC || event.getEventType() == FULL_GC || event.getEventType() == G1_MIXED_GC)
                    && event.getStartTime() > lastRemarkEndTime.get()) {
                if (event.getCollectionAgg() != null) {
                    data[2][3].add(event.getCollectionAgg().get(HUMONGOUS).getPreUsed());
                    data[4][3].add(event.getCollectionAgg().get(METASPACE).getPreUsed());
                }
                lastRemarkEndTime.set(Double.MAX_VALUE);
            }
        });
    }
}
