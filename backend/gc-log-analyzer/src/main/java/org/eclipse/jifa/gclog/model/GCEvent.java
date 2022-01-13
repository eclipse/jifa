/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
import org.eclipse.jifa.common.util.ErrorUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eclipse.jifa.gclog.vo.GCEventLevel.*;
import static org.eclipse.jifa.gclog.vo.HeapGeneration.*;

public class GCEvent {

    public static final double UNKNOWN_DOUBLE = -1d;
    public static final int UNKNOWN_INT = -1;
    public static final long UNKNOWN_LONG = -1L;

    private int gcid = UNKNOWN_INT;

    // unit of all time is ms
    private double startTime = UNKNOWN_DOUBLE;
    private double startTimestamp = UNKNOWN_DOUBLE;
    // real time duration of event
    private double duration = UNKNOWN_DOUBLE;
    private CpuTime cpuTime;
    private ReferenceGC referenceGC;

    private GCEventType eventType = GCEventType.UNDEFINED;
    private String cause;

    private GCCollectionResult collectionResult;// record original info in log
    private Map<HeapGeneration, GCCollectionResultItem> collectionAgg;// aggregation result;

    // phases may contain more detailed info about an event. For simplicity, we put subphase of an event as
    // a direct child in phases field rather than child of child. Use level field of EventType to check
    // if an event type should be a subphase.
    private List<GCEvent> phases;
    private List<GCSpecialSituation> specialSituations;

    private double pause = UNKNOWN_DOUBLE;
    private double interval = UNKNOWN_DOUBLE;

    private int promotion = UNKNOWN_INT;
    private int allocation = UNKNOWN_INT;
    private int reclamation = UNKNOWN_INT;

    public void setCollectionResult(GCCollectionResult collectionResult) {
        this.collectionResult = collectionResult;
    }

    public GCCollectionResult getCollectionResult() {
        return collectionResult;
    }

    public GCCollectionResult getOrCreateCollectionResult() {
        if (collectionResult == null) {
            collectionResult = new GCCollectionResult();
        }
        return collectionResult;
    }

    public List<GCEvent> getPhases() {
        return phases;
    }

    public GCEvent() {
    }

    public GCEvent(int gcid) {
        this.gcid = gcid;
    }

    public void setGcid(int gcid) {
        this.gcid = gcid;
    }

    public GCEvent getLastPhaseOfType(GCEventType type) {
        if (phases == null) {
            return null;
        }
        for (int i = phases.size() - 1; i >= 0; i--) {
            GCEvent phase = phases.get(i);
            if (phase.getEventType().equals(type)) {
                return phase;
            }
        }
        return null;
    }

    public ReferenceGC getReferenceGC() {
        return referenceGC;
    }

    public void setReferenceGC(ReferenceGC referenceGC) {
        this.referenceGC = referenceGC;
    }

    public int getGcid() {
        return gcid;
    }

    public GCEventType getEventType() {
        return eventType;
    }

    public double getStartTime() {
        return startTime;
    }

    public boolean isYoungGC() {
        return this.eventType != null && this.eventType.isYoungGC();
    }

    public boolean isOldGC() {
        return this.eventType != null && this.eventType.isOldGC();
    }

    public boolean isFullGC() {
        return this.eventType != null && this.eventType.isFullGC();
    }

    // use duration rather than cpu time
    public double getDuration() {
        return duration;
    }

    public List<GCSpecialSituation> getSpecialSituations() {
        return specialSituations;
    }

    public void setSpecialSituations(List<GCSpecialSituation> specialSituations) {
        this.specialSituations = specialSituations;
    }

    public void addSpecialSituation(GCSpecialSituation specialSituation) {
        if (specialSituation == null) {
            return;
        }
        if (specialSituations == null) {
            specialSituations = new ArrayList<>(2);
        }
        specialSituations.add(specialSituation);
    }

    public String getCause() {
        return cause;
    }

    public CpuTime getCpuTime() {
        return cpuTime;
    }

    public double getEndTime() {
        if (getStartTime() != UNKNOWN_DOUBLE && getDuration() != UNKNOWN_DOUBLE) {
            return getStartTime() + getDuration();
        } else {
            return UNKNOWN_DOUBLE;
        }
    }

    public int getAllocation() {
        return allocation;
    }

    public int getReclamation() {
        return reclamation;
    }

    public void setPromotion(int promotion) {
        if (promotion < 0) {
            promotion = 0;
        }
        this.promotion = promotion;
    }

    public void setAllocation(int allocation) {
        if (allocation < 0) {
            allocation = 0;
        }
        this.allocation = allocation;
    }

    public void setReclamation(int reclamation) {
        if (reclamation < 0) {
            reclamation = 0;
        }
        this.reclamation = reclamation;
    }

    public void setEventType(GCEventType eventType) {
        this.eventType = eventType;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public void setCpuTime(CpuTime cpuTime) {
        this.cpuTime = cpuTime;
    }

    public boolean hasPhases() {
        return phases != null;
    }

    public void addPhase(GCEvent phase) {
        if (phases == null) {
            phases = new ArrayList<>(2);
        }
        phases.add(phase);
    }

    public void setPhases(List<GCEvent> phases) {
        this.phases = phases;
    }

    public boolean hasPromotion() {
        return eventType != null && eventType.hasObjectPromotion();
    }

    public double getInterval() {
        return interval;
    }

    public void setInterval(double interval) {
        this.interval = interval;
    }

    public double getEndTimestamp() {
        if (getStartTimestamp() == UNKNOWN_DOUBLE || getDuration() == UNKNOWN_DOUBLE) {
            return UNKNOWN_DOUBLE;
        } else {
            return getStartTimestamp() + getDuration();
        }
    }

    public void setStartTimestamp(double startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public double getStartTimestamp() {
        return startTimestamp;
    }

    public double getPause() {
        if (pause == UNKNOWN_DOUBLE) {
            switch (eventType.getPause()) {
                case PAUSE:
                    pause = getDuration();
                    break;
                case CONCURRENT:
                    pause = 0;
                    break;
                case PARTIAL:
                    pause = 0;
                    if (phases != null) {
                        for (GCEvent phase : phases) {
                            if (phase.getEventType().getPause() == GCPause.PAUSE && phase.getEventLevel() == PHASE) {
                                pause += phase.getPause();
                            }
                        }
                    }
                    break;
                default:
                    ErrorUtil.shouldNotReachHere();
            }
        }
        return pause;
    }

    public int getPromotion() {
        return promotion;
    }

    public Map<HeapGeneration, GCCollectionResultItem> getCollectionAgg() {
        return collectionAgg;
    }

    public void setCollectionAgg(Map<HeapGeneration, GCCollectionResultItem> collectionAgg) {
        this.collectionAgg = collectionAgg;
    }

    public boolean hasSpecialSituation(GCSpecialSituation situation) {
        if (specialSituations == null) {
            return false;
        }
        for (GCSpecialSituation specialSituation : specialSituations) {
            if (specialSituation == situation) {
                return true;
            }
        }
        return false;
    }

    private static final HeapGeneration[] TO_STRING_GENERATION_ORDER = {YOUNG, OLD, HUMONGOUS, TOTAL, METASPACE};

    protected void appendStartTime(StringBuilder sb) {
        if (startTimestamp != UNKNOWN_DOUBLE) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            sb.append(format.format((long) getStartTimestamp())).append(" ");
        }

        if (startTime != UNKNOWN_DOUBLE) {
            sb.append(String.format("%.3f: ", getStartTime() / 1000));
        }
    }

    @Override
    public String toString() {
        // reference format: (1)14.244: [Full GC (Ergonomics), 0.001s] [Young: 2548K->0K(18944K)] [Old: 33595K->11813K(44032K)] [Total: 36143K->11813K(62976K)] [Metaspace: 19355K->19355K(1067008K)] [promotion 3000 K, interval 30s]
        StringBuilder sb = new StringBuilder();
        if (gcid != UNKNOWN_INT) {
            sb.append('(').append(gcid).append(')');
        }

        appendStartTime(sb);
        sb.append("[");
        if (eventType != null) {
            sb.append(eventType);
        }
        if (cause != null) {
            sb.append(" (").append(cause).append(")");
        }
        if (specialSituations != null && specialSituations.size() > 0) {
            sb.append(" (");
            for (int i = 0; i < specialSituations.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(specialSituations.get(i));
            }
            sb.append(')');
        }
        if (getDuration() != UNKNOWN_DOUBLE) {
            sb.append(", ").append(String.format("%.3f", getDuration() / 1000)).append("s");
        }
        sb.append("]");
        boolean memoryInfoAvailable = collectionAgg != null;
        if (memoryInfoAvailable) {
            for (HeapGeneration generation : TO_STRING_GENERATION_ORDER) {
                GCCollectionResultItem item = collectionAgg.get(generation);
                if (!item.isEmpty()) {
                    sb.append(" [").append(item).append(']');
                }
            }
        }

        boolean moreInfoAvailable = getEventLevel() == EVENT
                && (getPromotion() != UNKNOWN_INT || getInterval() != UNKNOWN_DOUBLE);
        if (moreInfoAvailable) {
            boolean first = true;
            sb.append(" [");
            if (getPromotion() != UNKNOWN_INT) {
                sb.append("promotion ").append(getPromotion()).append(" K");
                first = false;
            }
            if (getInterval() != UNKNOWN_INT) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append("interval ").append(String.format("%.3f", getInterval() / 1000)).append("s");
            }
            sb.append("]");
        }
        if (cpuTime != null) {
            sb.append(" [").append(cpuTime).append("]");
        }
        if (getEventType().getPause() == GCPause.PARTIAL && getPhases() != null) {
            for (GCEvent phase : getPhases()) {
                if (phase.getEventLevel() != GCEventLevel.SUBPHASE && phase.getEventType().getPause() == GCPause.PAUSE) {
                    sb.append(" [")
                            .append(phase.getEventType().getName())
                            .append(" ")
                            .append(String.format("%.3f", phase.getPause() / 1000))
                            .append("s]");
                }
            }
        }
        return sb.toString();
    }

    public GCEventLevel getEventLevel() {
        return eventType.getLevel();
    }
}
