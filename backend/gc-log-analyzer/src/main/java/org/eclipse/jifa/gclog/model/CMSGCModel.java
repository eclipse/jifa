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
import org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType;
import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.jifa.gclog.util.LongData;
import org.eclipse.jifa.gclog.vo.TimeRange;

import java.util.List;

import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_LONG;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.METASPACE;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.OLD;
import static org.eclipse.jifa.gclog.model.GCEventType.*;

public class CMSGCModel extends GenerationalGCModel {
    private static GCCollectorType collector = GCCollectorType.CMS;

    public CMSGCModel() {
        super(collector);
    }

    private static List<GCEventType> allEventTypes = GCModel.calcAllEventTypes(collector);
    private static List<GCEventType> pauseEventTypes = GCModel.calcPauseEventTypes(collector);
    private static List<GCEventType> mainPauseEventTypes = GCModel.calcMainPauseEventTypes(collector);
    private static List<GCEventType> parentEventTypes = GCModel.calcParentEventTypes(collector);
    private static List<GCEventType> importantEventTypes = List.of(YOUNG_GC, FULL_GC, CMS_CONCURRENT_MARK_SWEPT,
            CMS_INITIAL_MARK, CMS_CONCURRENT_ABORTABLE_PRECLEAN, CMS_FINAL_REMARK, CMS_CONCURRENT_SWEEP);


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

    @Override
    protected void doAfterCalculatingDerivedInfo() {
        decideGCsAfterOldGC();
    }

    private void decideGCsAfterOldGC() {
        double lastCMSEndTime = Double.MAX_VALUE;
        double lastRemarkEndTime = Double.MAX_VALUE;
        for (GCEvent event : getGcEvents()) {
            if (event.getEventType() == CMS_CONCURRENT_MARK_SWEPT) {
                if (!event.containPhase(CMS_CONCURRENT_FAILURE) &&
                        !event.containPhase(CMS_CONCURRENT_INTERRUPTED)) {
                    lastCMSEndTime = event.getEndTime();
                }
                GCEvent remark = event.getLastPhaseOfType(CMS_FINAL_REMARK);
                if (remark != null) {
                    lastRemarkEndTime = remark.getEndTime();
                }

            } else if ((event.getEventType() == YOUNG_GC || event.getEventType() == FULL_GC)) {
                if (event.getStartTime() > lastRemarkEndTime) {
                    event.setTrue(GCEventBooleanType.GC_AFTER_REMARK);
                    lastRemarkEndTime = Double.MAX_VALUE;
                }
                if (event.getStartTime() > lastCMSEndTime) {
                    // Although jdk11 prints old gen usage after old gc, still use
                    // the next gc result for convenience
                    event.setTrue(GCEventBooleanType.GC_AT_END_OF_OLD_CYCLE);
                    lastCMSEndTime = Double.MAX_VALUE;
                }
            }
        }
    }

    @Override
    protected void calculateUsedAvgAfterOldGC(TimeRange range, LongData[][] data) {
        iterateEventsWithinTimeRange(getGcEvents(), range, event -> {
            if (event.isTrue(GCEventBooleanType.GC_AT_END_OF_OLD_CYCLE) && event.getMemoryItem(OLD) != null) {
                data[1][3].add(event.getMemoryItem(OLD).getPreUsed());
            }
            if (event.isTrue(GCEventBooleanType.GC_AFTER_REMARK) && event.getMemoryItem(METASPACE) != null) {
                data[4][3].add(event.getMemoryItem(METASPACE).getPreUsed());
            }
        });
    }
}
