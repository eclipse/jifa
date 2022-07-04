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
import org.eclipse.jifa.gclog.vo.GCCollectorType;
import org.eclipse.jifa.gclog.vo.TimeRange;

import java.util.Arrays;
import java.util.List;

import static org.eclipse.jifa.gclog.model.GCEvent.UNKNOWN_LONG;
import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.vo.HeapGeneration.METASPACE;
import static org.eclipse.jifa.gclog.vo.HeapGeneration.OLD;

public class CMSGCModel extends GenerationalGCModel {

    private final static List<GCEventType> SUPPORTED_PHASE_EVENT_TYPES = Arrays.asList(
            YOUNG_GC,
            FULL_GC,
            CMS_CONCURRENT_MARK_SWEPT,
            CMS_INITIAL_MARK,
            CMS_CONCURRENT_MARK,
            CMS_CONCURRENT_PRECLEAN,
            CMS_CONCURRENT_ABORTABLE_PRECLEAN,
            CMS_REMARK,
            CMS_CONCURRENT_SWEEP,
            CMS_CONCURRENT_RESET,
            CMS_CONCURRENT_INTERRUPTED
    );

    @Override
    protected List<GCEventType> getSupportedPhaseEventTypes() {
        return SUPPORTED_PHASE_EVENT_TYPES;
    }

    private final static List<String> PAUSE_EVENT_NAMES = Arrays.asList(
            YOUNG_GC.getName(),
            FULL_GC.getName(),
            CMS_INITIAL_MARK.getName(),
            CMS_REMARK.getName()
    );

    @Override
    protected List<String> getPauseEventNames() {
        return PAUSE_EVENT_NAMES;
    }

    public CMSGCModel() {
        super(GCCollectorType.CMS);
    }

    private final static List<String> METADATA_EVENT_TYPES = Arrays.asList(
            YOUNG_GC.getName(),
            FULL_GC.getName(),
            CMS_CONCURRENT_MARK_SWEPT.getName()
    );

    @Override
    protected List<String> getMetadataEventTypes() {
        return METADATA_EVENT_TYPES;
    }

    @Override
    protected void calculateUsedAvgAfterOldGC(TimeRange range, LongData[][] data) {
        // We first try to read it from "Concurrent Sweep" event, then read it from the
        // first young or full gc after cms cycle
        AtomicDouble lastCMSEndTime = new AtomicDouble(Double.MAX_VALUE);
        iterateEventsWithinTimeRange(getGcEvents(), range, event -> {
            if (event.getEventType() == CMS_CONCURRENT_MARK_SWEPT) {
                if (event.getLastPhaseOfType(CMS_CONCURRENT_FAILURE) != null ||
                        event.getLastPhaseOfType(CMS_CONCURRENT_INTERRUPTED) != null) {
                    return;
                }
                GCEvent swept = event.getLastPhaseOfType(CMS_CONCURRENT_SWEEP);
                if (swept != null && swept.getCollectionAgg() != null) {
                    long usedAfterGC = swept.getCollectionAgg().get(OLD).getPostUsed();
                    if (usedAfterGC != UNKNOWN_LONG) {
                        data[1][3].add(usedAfterGC);
                        return;
                    }
                }
                lastCMSEndTime.set(event.getEndTime());
            } else if ((event.getEventType() == YOUNG_GC || event.getEventType() == FULL_GC)
                    && event.getStartTime() > lastCMSEndTime.get()) {
                if (event.getCollectionAgg() != null) {
                    data[1][3].add(event.getCollectionAgg().get(OLD).getPreUsed());
                    data[4][3].add(event.getCollectionAgg().get(METASPACE).getPreUsed());
                }
                lastCMSEndTime.set(Double.MAX_VALUE);
            }
        });
    }
}
