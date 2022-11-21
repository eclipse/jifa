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
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea;
import org.eclipse.jifa.gclog.event.evnetInfo.GCMemoryItem;
import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.jifa.gclog.model.modeInfo.GCLogStyle;
import org.eclipse.jifa.gclog.util.LongData;
import org.eclipse.jifa.gclog.vo.TimeRange;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_INT;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.*;
import static org.eclipse.jifa.gclog.model.GCEventType.*;

public class G1GCModel extends GCModel {
    private long heapRegionSize = UNKNOWN_INT;   // in b
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
            G1_CONCURRENT_MARK, G1_REMARK, G1_CONCURRENT_REBUILD_REMEMBERED_SETS, G1_PAUSE_CLEANUP, G1_CONCURRENT_UNDO_CYCLE);

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
        return (type == YOUNG_GC || type == FULL_GC || type == G1_MIXED_GC) && event.getMemoryItems() != null;
    }

    private static List<MemoryArea> AREAS_COUNTED_BY_REGION = List.of(EDEN, SURVIVOR, OLD, HUMONGOUS, ARCHIVE);

    private void inferHeapRegionSize() {
        if (heapRegionSize != UNKNOWN_INT) {
            return;
        }
        for (int i = getGcEvents().size() - 1; i >= 0; i--) {
            GCEvent event = getGcEvents().get(i);
            if (!collectionResultUsingRegion(event)) {
                continue;
            }
            if (event.getMemoryItem(HEAP).getPreUsed() == UNKNOWN_INT) {
                continue;
            }
            long regionCount = Arrays.stream(event.getMemoryItems())
                    .filter(item -> item != null && AREAS_COUNTED_BY_REGION.contains(item.getArea())
                            && item.getPreUsed() != UNKNOWN_INT)
                    .mapToLong(GCMemoryItem::getPreUsed)
                    .sum();
            if (regionCount < 3) {
                continue;
            }
            double bytesPerRegion = event.getMemoryItem(HEAP).getPreUsed() / (double) regionCount;
            heapRegionSize = (long) Math.pow(2, Math.ceil(Math.log(bytesPerRegion) / Math.log(2)));
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
            for (GCMemoryItem item : event.getMemoryItems()) {
                if (item != null && AREAS_COUNTED_BY_REGION.contains(item.getArea())) {
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
                    if (mixedGC.getMemoryItem(OLD) != null) {
                        data[1][3].add(mixedGC.getMemoryItem(OLD).getPostUsed());
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
                if (remark != null) {
                    lastRemarkEndTime.set(remark.getEndTime());
                }
            } else if ((event.getEventType() == YOUNG_GC || event.getEventType() == FULL_GC || event.getEventType() == G1_MIXED_GC)
                    && event.getStartTime() > lastRemarkEndTime.get()) {
                if (event.getMemoryItem(HUMONGOUS) != null) {
                    data[2][3].add(event.getMemoryItem(HUMONGOUS).getPreUsed());

                }
                if (event.getMemoryItem(METASPACE) != null) {
                    data[4][3].add(event.getMemoryItem(METASPACE).getPreUsed());
                }
                lastRemarkEndTime.set(Double.MAX_VALUE);
            }
        });
    }
}
