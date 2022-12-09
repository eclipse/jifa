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

package org.eclipse.jifa.gclog.diagnoser;


import org.eclipse.jifa.gclog.event.GCEvent;

import org.eclipse.jifa.gclog.event.evnetInfo.*;
import org.eclipse.jifa.gclog.model.GCEventType;
import org.eclipse.jifa.gclog.model.GCModel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jifa.gclog.diagnoser.AbnormalType.*;
import static org.eclipse.jifa.gclog.diagnoser.AbnormalType.TO_SPACE_EXHAUSTED;
import static org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType.*;
import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.*;
import static org.eclipse.jifa.gclog.util.Constant.*;

// This class detects abnormals that will be displayed in gc detail in frontend. Their cause and suggestion will
// be found in the next step in EventSuggestionGenerator.
public class EventAbnormalDetector {
    private static List<Method> eventDiagnoseRules = new ArrayList<>();

    private GCModel model;
    private AnalysisConfig config;
    private GlobalDiagnoseInfo globalDiagnoseInfo;

    public EventAbnormalDetector(GCModel model, AnalysisConfig config, GlobalDiagnoseInfo globalDiagnoseInfo) {
        this.model = model;
        this.config = config;
        this.globalDiagnoseInfo = globalDiagnoseInfo;
    }

    public void diagnose() {
        callDiagnoseRules();
    }

    private void callDiagnoseRules() {
        // The order matters.
        duration();
        eventType();
        gcCause();
        specialSituation();
        memory();
        cputime();
        promotion();
        interval();
    }

    private void addAbnormal(GCEvent event, AbnormalType type) {
        EventAbnormalSet set = globalDiagnoseInfo.getEventDiagnoseInfo(event).getAbnormals();
        set.add(new AbnormalPoint(type, event));
    }

    private void duration() {
        for (GCEvent event : model.getAllEvents()) {
            boolean pause = event.isPause();
            double threshold = pause ? config.getLongPauseThreshold() : config.getLongConcurrentThreshold();
            double actual = pause ? event.getPause() : event.getDuration();
            if (actual == UNKNOWN_DOUBLE) {
                return;
            }
            if (actual >= threshold) {
                addAbnormal(event, BAD_DURATION);
            }
        }
    }

    protected void eventType() {
        for (GCEvent event : model.getAllEvents()) {
            if (event.getEventType().isBad()) {
                addAbnormal(event, BAD_EVENT_TYPE);
            }
        }
    }

    protected void gcCause() {
        for (GCEvent event : model.getGcEvents()) {
            if (event.getCause() != null && event.isBadFullGC()) {
                addAbnormal(event, BAD_CAUSE_FULL_GC);
            }
        }
        // todo: we should warn user if humongous or gclocker gc is frequent
    }

    protected void specialSituation() {
        for (GCEvent event : model.getGcEvents()) {
            if (event.isTrue(GCEventBooleanType.TO_SPACE_EXHAUSTED)) {
                addAbnormal(event, TO_SPACE_EXHAUSTED);
            }
        }
    }

    protected void memory() {
        List<MemoryArea> areasToCheck = new ArrayList<>(5);
        if (model.isGenerational()) {
            areasToCheck.add(YOUNG);
            areasToCheck.add(OLD);
        }
        if (model.hasHumongousArea()) {
            areasToCheck.add(HUMONGOUS);
        }
        areasToCheck.add(HEAP);
        areasToCheck.add(METASPACE);


        for (GCEvent event : model.getGcCollectionEvents()) {
            GCMemoryItem heap = event.getMemoryItem(HEAP);
            long heapCapacity = heap != null ? heap.getPostCapacity() : UNKNOWN_INT;
            for (MemoryArea area : areasToCheck) {
                GCMemoryItem memory = event.getMemoryItem(area);
                if (memory == null) {
                    continue;
                }

                // post capacity
                long capacity = memory.getPostCapacity();
                if (capacity != UNKNOWN_INT && heapCapacity != UNKNOWN_INT && (area == YOUNG || area == OLD)) {
                    // only check one case: generation capacity is too small
                    long threshold = (long) (config.getSmallGenerationThreshold() * heapCapacity / 100);
                    if (capacity <= threshold) {
                        addAbnormal(event, area == YOUNG ? BAD_YOUNG_GEN_CAPACITY : BAD_OLD_GEN_CAPACITY);
                    }
                }

                // post used
                long postUsed = memory.getPostUsed();
                if (area == YOUNG || postUsed == UNKNOWN_INT) {
                    continue;
                }
                // check one case: used is too high after gc
                if (area == OLD) {
                    // FIXME: in JDK8, printed g1 post capacity may be much smaller than eden preused of the next
                    //  young gc. Maybe we need to improve this check
                    if (capacity != UNKNOWN_INT && (event.isFullGC() || event.isTrue(GC_AT_END_OF_OLD_CYCLE))) {
                        long threshold = (long) (capacity * config.getHighOldUsageThreshold() / 100);
                        if (postUsed > threshold) {
                            addAbnormal(event, BAD_OLD_USED);
                        }
                    }
                } else if (area == HUMONGOUS) {
                    if (heapCapacity != UNKNOWN_INT) {
                        long threshold = (long) (heapCapacity * config.getHighHumongousUsageThreshold() / 100);
                        if (postUsed >= threshold) {
                            addAbnormal(event, BAD_HUMONGOUS_USED);
                        }
                    }
                } else if (area == HEAP) {
                    if (capacity != UNKNOWN_INT && (event.isFullGC() || event.isTrue(GC_AT_END_OF_OLD_CYCLE))) {
                        long threshold = (long) (capacity * config.getHighOldUsageThreshold() / 100);
                        if (postUsed > threshold) {
                            addAbnormal(event, BAD_HEAP_USED);
                        }
                    }
                } else if (area == METASPACE) {
                    if (capacity != UNKNOWN_INT && (event.isFullGC() || event.isTrue(GC_AFTER_REMARK))) {
                        long threshold = (long) (capacity * config.getHighMetaspaceUsageThreshold() / 100);
                        if (postUsed > threshold) {
                            addAbnormal(event, BAD_METASPACE_USED);
                        }
                    }
                }
            }
        }
    }

    protected void cputime() {
        for (GCEvent event : model.getAllEvents()) {
            CpuTime cpuTime = event.getCpuTime();
            // we rarely care about cpu time of concurrent phase
            if (cpuTime == null || event.isPause()) {
                continue;
            }
            if (!globalDiagnoseInfo.getEventDiagnoseInfo(event)
                    .getAbnormals().contains(BAD_DURATION)) {
                continue;
            }
            double real = cpuTime.getReal();
            double sys = cpuTime.getSys();
            double usr = cpuTime.getUser();
            if (sys / real >= config.getHighSysThreshold() / 100) {
                addAbnormal(event, BAD_SYS);
            }
            if (usr / real <= config.getLowUsrThreshold() / 100) {
                addAbnormal(event, BAD_USR);
            }
        }
    }

    protected void promotion() {
        if (!model.isGenerational()) {
            return;
        }
        for (GCEvent event : model.getGcCollectionEvents()) {
            long promotion = event.getPromotion();
            if (promotion == UNKNOWN_INT) {
                continue;
            }
            GCMemoryItem old = event.getMemoryItem(OLD);
            if (old.getPostCapacity() == UNKNOWN_INT) {
                continue;
            }
            long threshold = (long) (old.getPostCapacity() * config.getHighPromotionThreshold() / 100);
            if (promotion >= threshold) {
                addAbnormal(event, BAD_PROMOTION);
            }
        }
    }

    protected void interval() {
        for (GCEvent event : model.getAllEvents()) {
            double actual = event.getInterval();
            if (actual == UNKNOWN_DOUBLE) {
                continue;
            }
            GCEventType eventType = event.getEventType();
            double threshold;
            if (eventType.isYoungGC()) {
                threshold = config.getYoungGCFrequentIntervalThreshold();
            } else if (eventType.isOldGC()) {
                threshold = config.getOldGCFrequentIntervalThreshold();
            } else if (event.isFullGC()) {
                threshold = config.getFullGCFrequentIntervalThreshold();
            } else {
                continue;
            }
            if (actual <= threshold) {
                addAbnormal(event, BAD_INTERVAL);
            }
        }
    }
}
