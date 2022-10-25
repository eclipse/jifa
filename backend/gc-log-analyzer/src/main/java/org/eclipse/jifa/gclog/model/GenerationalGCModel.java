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

import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.event.evnetInfo.GCCause;
import org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea;
import org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType;
import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.jifa.gclog.model.modeInfo.GCLogStyle;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jifa.gclog.model.GCEventType.FULL_GC;
import static org.eclipse.jifa.gclog.model.GCEventType.YOUNG_GC;

public abstract class GenerationalGCModel extends GCModel {
    public GenerationalGCModel(GCCollectorType type) {
        super(type);
    }

    private void dealYoungGCThatBecomeFullGCUnified() {
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
                event.setTrue(GCEventBooleanType.YOUNG_GC_BECOME_FULL_GC);
                event.setPhases(nextEvent.getPhases());
                i++; // remove the full gc
            }
            if (event.getEventType() == FULL_GC && event.isTrue(GCEventBooleanType.PROMOTION_FAILED)) {
                event.setCause(GCCause.PROMOTION_FAILED);
            }
            newEvents.add(event);
        }
        if (!remove) {
            newEvents.add(oldEvents.get(oldEvents.size() - 1));
        }
        setGcEvents(newEvents);
    }

    private void dealYoungGCThatBecomeFullGCPreUnified() {
        for (GCEvent event : getGcEvents()) {
            // if metaspace is printed, it must be a full gc
            if (event.getEventType() == YOUNG_GC && event.getMemoryItem(MemoryArea.METASPACE) != null) {
                event.setEventType(FULL_GC);
                event.setTrue(GCEventBooleanType.YOUNG_GC_BECOME_FULL_GC);
            }
            if (event.getEventType() == FULL_GC && event.isTrue(GCEventBooleanType.PROMOTION_FAILED)) {
                event.setCause(GCCause.PROMOTION_FAILED);
            }
        }
    }

    private void youngGenUsedShouldBeZeroAfterFullGC() {
        if (getLogStyle() != GCLogStyle.PRE_UNIFIED) {
            return;
        }
        for (GCEvent event : getGcEvents()) {
            if (event.getEventType() == FULL_GC && event.getMemoryItem(MemoryArea.YOUNG) != null) {
                event.getMemoryItem(MemoryArea.YOUNG).setPostUsed(0);
            }
        }
    }

    @Override
    protected void doBeforeCalculatingDerivedInfo() {
        if (getLogStyle() == GCLogStyle.UNIFIED) {
            dealYoungGCThatBecomeFullGCUnified();
        } else if (getLogStyle() == GCLogStyle.PRE_UNIFIED) {
            dealYoungGCThatBecomeFullGCPreUnified();
        }
        youngGenUsedShouldBeZeroAfterFullGC();
    }
}
