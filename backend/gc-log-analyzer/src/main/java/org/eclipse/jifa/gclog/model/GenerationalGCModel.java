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

import org.eclipse.jifa.gclog.vo.*;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jifa.gclog.model.GCEventType.FULL_GC;
import static org.eclipse.jifa.gclog.model.GCEventType.YOUNG_GC;

public abstract class GenerationalGCModel extends GCModel {
    public GenerationalGCModel(GCCollectorType type) {
        super(type);
    }

    private void removeYoungGCThatBecomeFullGC() {
        if (getLogStyle() != GCLogStyle.UNIFIED) {
            return;
        }
        List<GCEvent> newEvents = new ArrayList<>();
        List<GCEvent> oldEvents = getGcEvents();
        boolean remove = false;
        for (int i = 0; i < oldEvents.size() - 1; i++) {
            GCEvent event = oldEvents.get(i);
            GCEvent nextEvent = oldEvents.get(i + 1);
            remove = event.getEventType() == YOUNG_GC && nextEvent.getEventType() == FULL_GC &&
                    event.getStartTime() <= nextEvent.getStartTime() && event.getEndTime() >= nextEvent.getEndTime();
            if (remove) {
                event.setEventType(FULL_GC);
                event.setPhases(nextEvent.getPhases());
                i++; // remove the full gc
            }
            newEvents.add(event);
        }
        if (!remove) {
            newEvents.add(oldEvents.get(oldEvents.size() - 1));
        }
        setGcEvents(newEvents);
    }

    private void fixYoungGCPromotionFail() {
        for (GCEvent event : getGcEvents()) {
            if (event.getEventType() == YOUNG_GC && event.hasSpecialSituation(GCSpecialSituation.PROMOTION_FAILED)) {
                // when there is promotion fail, overwrite its original gccause with promotion failed
                event.setEventType(FULL_GC);
                event.setCause(GCSpecialSituation.PROMOTION_FAILED.getName());
                event.getSpecialSituations().remove(GCSpecialSituation.PROMOTION_FAILED);
            }
        }
    }

    private void youngGenUsedShouldBeZeroAfterFullGC() {
        if (getLogStyle() != GCLogStyle.PRE_UNIFIED) {
            return;
        }
        for (GCEvent event : getGcEvents()) {
            if (event.getEventType() == FULL_GC && event.getCollectionResult() != null) {
                for (GCCollectionResultItem item : event.getCollectionResult().getItems()) {
                    if (item.getGeneration() == HeapGeneration.YOUNG) {
                        item.setPostUsed(0);
                    }
                }
            }
        }
    }

    @Override
    protected void doBeforeCalculatingDerivedInfo() {
        removeYoungGCThatBecomeFullGC();
        fixYoungGCPromotionFail();
        youngGenUsedShouldBeZeroAfterFullGC();
    }
}
