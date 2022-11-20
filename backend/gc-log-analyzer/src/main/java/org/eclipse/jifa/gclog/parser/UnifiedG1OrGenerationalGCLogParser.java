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

package org.eclipse.jifa.gclog.parser;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.event.evnetInfo.*;
import org.eclipse.jifa.gclog.model.GCEventType;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext;
import org.eclipse.jifa.gclog.parser.ParseRule.PrefixAndValueParseRule;
import org.eclipse.jifa.gclog.util.Constant;
import org.eclipse.jifa.gclog.util.GCLogUtil;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext.GCID;
import static org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext.UPTIME;

public abstract class UnifiedG1OrGenerationalGCLogParser extends AbstractUnifiedGCLogParser {
    private static List<ParseRule> withoutGCIDRules;
    private static List<ParseRule> withGCIDRules;

    public static List<ParseRule> getSharedWithoutGCIDRules() {
        return withoutGCIDRules;
    }

    public static List<ParseRule> getSharedWithGCIDRules() {
        return withGCIDRules;
    }

    static {
        initializeParseRules();
    }

    private static void initializeParseRules() {
        withoutGCIDRules = new ArrayList<>(AbstractUnifiedGCLogParser.getSharedWithoutGCIDRules());

        withGCIDRules = new ArrayList<>(AbstractUnifiedGCLogParser.getSharedWithGCIDRules());
        withGCIDRules.add(new PrefixAndValueParseRule("Metaspace:", UnifiedG1OrGenerationalGCLogParser::parseMetaspace));
        withGCIDRules.add(UnifiedG1OrGenerationalGCLogParser::parseHeap);
        withGCIDRules.add(new PrefixAndValueParseRule("Pause Young", UnifiedG1OrGenerationalGCLogParser::parseYoungFullGC));
        withGCIDRules.add(new PrefixAndValueParseRule("Pause Full", UnifiedG1OrGenerationalGCLogParser::parseYoungFullGC));
        withGCIDRules.add(UnifiedG1OrGenerationalGCLogParser::parseWorker);
        withGCIDRules.add(UnifiedG1OrGenerationalGCLogParser::parseCpuTime);
        // subclass will add more rules
    }

    protected abstract List<ParseRule> getWithoutGCIDRules();

    protected abstract List<ParseRule> getWithGCIDRules();

    private static boolean parseCpuTime(AbstractGCLogParser parser, ParseRuleContext context, String text) {
        GCModel model = parser.getModel();
        //[0.524s][info   ][gc,cpu       ] GC(0) User=22.22s Sys=23.23s Real=24.24s
        if (!text.startsWith("User=") || !text.endsWith("s")) {
            return false;
        }
        CpuTime cpuTime = GCLogUtil.parseCPUTime(text);
        GCEvent event = model.getLastEventOfGCID(context.get(GCID));
        if (event != null) {
            event = ((UnifiedG1OrGenerationalGCLogParser) parser).getCPUTimeEventOrPhase(event);
            if (event != null) {
                event.setCpuTime(cpuTime);
            }
        }
        return true;
    }

    protected abstract GCEvent getCPUTimeEventOrPhase(GCEvent event);

    @Override
    protected final void doParseLineWithoutGCID(String detail, double uptime) {
        ParseRuleContext context = new ParseRuleContext();
        context.put(UPTIME, uptime);
        doParseUsingRules(this, context, detail, getWithoutGCIDRules());
    }

    @Override
    protected final void doParseLineWithGCID(String detail, int gcid, double uptime) {
        ParseRuleContext context = new ParseRuleContext();
        context.put(UPTIME, uptime);
        context.put(GCID, gcid);
        doParseUsingRules(this, context, detail, getWithGCIDRules());
    }

    /**
     * for reference
     * [0.501s][info   ][gc,start     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
     * [0.524s][info   ][gc           ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 18M->19M(20M) 21.21ms
     * [6.845s][info][gc,start      ] GC(26) Pause Full (System.gc())
     * [0.276s][info ][gc,start     ] GC(34) Pause Young (Allocation Failure)
     * [10.115s][info ][gc,start     ] GC(25) Pause Full (Ergonomics)
     * [15.732s][info][gc,start       ] GC(42) Pause Young (Mixed) (G1 Evacuation Pause)
     * [56.810s][info][gc,start      ] GC(33) Pause Young (Concurrent Start) (GCLocker Initiated GC)
     */
    private static void parseYoungFullGC(AbstractGCLogParser parser, ParseRuleContext context, String title, String text) {
        GCModel model = parser.getModel();
        String[] parts = GCLogUtil.splitByBracket(text);
        int causeIndex = 0;
        GCEventType eventType = title.endsWith("Young") ? YOUNG_GC : FULL_GC;
        GCEventBooleanType specialSituation = null;
        if (parser.getMetadata().getCollector() == GCCollectorType.G1 && eventType == YOUNG_GC) {
            switch (parts[0]) {
                case "Concurrent Start":
                    specialSituation = GCEventBooleanType.INITIAL_MARK;
                    break;
                case "Prepare Mixed":
                    specialSituation = GCEventBooleanType.PREPARE_MIXED;
                    break;
                case "Mixed":
                    eventType = G1_MIXED_GC;
                    break;
            }
            causeIndex++;
        }
        GCCause cause = GCCause.getCause(parts[causeIndex]);
        boolean end = text.endsWith("ms");
        GCEvent event;
        if (!end || (event = model.getLastEventOfGCID(context.get(GCID))) == null) {
            event = new GCEvent();
            event.setStartTime(context.get(UPTIME));
            event.setEventType(eventType);
            event.setCause(cause);
            if (cause == GCCause.CMS_FINAL_REMARK) {
                event.setTrue(GCEventBooleanType.IGNORE_PAUSE);
            }
            if (specialSituation != null) {
                event.setTrue(specialSituation);
            }
            event.setGcid(context.get(GCID));
            model.putEvent(event);
        }
        if (end) {
            int tailBegin = text.lastIndexOf(' ');
            tailBegin = text.lastIndexOf(' ', tailBegin - 1);
            if (tailBegin > 0) {
                parseCollectionAndDuration(event, context, text.substring(tailBegin + 1));
            }
        }
    }

    //  18M->19M(20M) 21.21ms
    protected static void parseCollectionAndDuration(GCEvent event, ParseRuleContext context, String s) {
        if (StringUtils.isBlank(s)) {
            return;
        }
        for (String part : s.split(" ")) {
            if (part.contains("->") && part.endsWith(")") && !part.startsWith("(")) {
                long[] memories = GCLogUtil.parseMemorySizeFromTo(part);
                GCMemoryItem item = new GCMemoryItem(MemoryArea.HEAP, memories);
                event.setMemoryItem(item);
            } else if (part.endsWith("ms")) {
                double duration = GCLogUtil.toMillisecond(part);
                event.setDuration(duration);
                if (event.getStartTime() == Constant.UNKNOWN_DOUBLE) {
                    event.setStartTime((double) context.get(UPTIME) - duration);
                }
            }
        }
    }

    /**
     * [0.524s][info   ][gc,heap      ] GC(0) Eden regions: 5->6(7)
     * [0.524s][info   ][gc,heap      ] GC(0) Survivor regions: 8->9(10)
     * [0.524s][info   ][gc,heap      ] GC(0) Old regions: 11->12
     * [0.524s][info   ][gc,heap      ] GC(0) Humongous regions: 13->14
     * [1.738s][info][gc,heap        ] GC(2) Archive regions: 2->2
     * [0.524s][info   ][gc,metaspace ] GC(0) Metaspace: 15K->16K(17K)
     * [2.285s][info ][gc,heap      ] GC(2) Old: 23127K->2019K(43712K)
     * [0.160s][info ][gc,heap      ] GC(0) ParNew: 17393K->2175K(19648K)
     * [0.160s][info ][gc,heap      ] GC(0) CMS: 0K->130K(43712K)
     * [0.194s][info][gc,heap     ] GC(0) DefNew: 40960K(46080K)->5120K(46080K) Eden: 40960K(40960K)->0K(40960K) From: 0K(5120K)->5120K(5120K)
     * [0.569s][info][gc,heap        ] GC(1) PSYoungGen: 6128K(45056K)->0K(45056K) Eden: 0K(38912K)->0K(38912K) From: 6128K(6144K)->0K(6144K)
     */
    private static boolean parseHeap(AbstractGCLogParser parser, ParseRuleContext context, String s) {
        GCModel model = parser.getModel();
        String[] parts = GCLogUtil.splitBySpace(s);
        if (parts.length != 2 && parts.length != 3 && parts.length != 6) {
            return false;
        }
        String generationName = parts[0];
        if (generationName.endsWith(":")) {
            generationName = generationName.substring(0, generationName.length() - 1);
        }
        MemoryArea generation = MemoryArea.getMemoryArea(generationName);
        if (generation == null) {
            return false;
        }
        // format check done

        GCEvent event = model.getLastEventOfGCID(context.get(GCID));
        if (event == null) {
            // log may be incomplete
            return true;
        }
        if (event.getEventType() == CMS_CONCURRENT_MARK_SWEPT) {
            event = event.getLastPhaseOfType(CMS_CONCURRENT_SWEEP);
            if (event == null) {
                return true;
            }
        }
        long[] memories = GCLogUtil.parseMemorySizeFromTo(parts.length == 3 ? parts[2] :parts[1], 1);
        // will multiply region size before calculating derived info for g1
        GCMemoryItem item = new GCMemoryItem(generation, memories);
        event.setMemoryItem(item);

        if (parts.length == 6) {
            event.setMemoryItem(new GCMemoryItem(MemoryArea.EDEN, GCLogUtil.parseMemorySizeFromTo(parts[3])));
            event.setMemoryItem(new GCMemoryItem(MemoryArea.SURVIVOR, GCLogUtil.parseMemorySizeFromTo(parts[5])));
        }
        return true;
    }


    /*
     * [0.160s][info ][gc,metaspace ] GC(0) Metaspace: 5147K->5147K(1056768K)
     * [0.194s][info][gc,metaspace] GC(0) Metaspace: 137K(384K)->138K(384K) NonClass: 133K(256K)->133K(256K) Class: 4K(128K)->4K(128K)
     */
    private static void parseMetaspace(AbstractGCLogParser parser, ParseRuleContext context, String title, String text) {
        GCModel model = parser.getModel();
        GCEvent event = model.getLastEventOfGCID(context.get(GCID));
        if (event == null) {
            // log may be incomplete
            return;
        }
        String[] parts = GCLogUtil.splitBySpace(text);
        event.setMemoryItem(new GCMemoryItem(MemoryArea.METASPACE, GCLogUtil.parseMemorySizeFromTo(parts[0])));
        if (parts.length == 5) {
            model.setMetaspaceCapacityReliable(true);
            event.setMemoryItem(new GCMemoryItem(MemoryArea.NONCLASS, GCLogUtil.parseMemorySizeFromTo(parts[2])));
            event.setMemoryItem(new GCMemoryItem(MemoryArea.CLASS, GCLogUtil.parseMemorySizeFromTo(parts[4])));
        }
    }

    /**
     * e.g.
     * [2.983s][info   ][gc,marking    ] GC(1) Concurrent Clear Claimed Marks
     * [2.983s][info   ][gc,marking    ] GC(1) Concurrent Clear Claimed Marks 25.25ms
     * [3.266s][info   ][gc,phases     ] GC(2) Phase 1: Mark live objects 50.50ms
     * [3.002s][info   ][gc            ] GC(1) Pause Cleanup 1480M->1480M(1700M) 41.41ms
     * <p>
     * two cases of phases in gclog: one line summary , two lines of begin and end
     */
    protected static void parsePhase(AbstractGCLogParser parser, ParseRuleContext context, String phaseName, String value) {
        GCModel model = parser.getModel();
        phaseName = phaseName.trim();
        GCEventType phaseType = ((UnifiedG1OrGenerationalGCLogParser) parser).getGCEventType(phaseName);
        boolean end = value.endsWith("ms");
        GCEvent event;
        // cms does not have a line to indicate its beginning, hard code here
        if (parser.getMetadata().getCollector() == GCCollectorType.CMS &&
                phaseType == CMS_INITIAL_MARK && !end) {
            event = new GCEvent();
            event.setEventType(CMS_CONCURRENT_MARK_SWEPT);
            event.setStartTime(context.get(UPTIME));
            event.setGcid(context.get(GCID));
            model.putEvent(event);
        } else {
            event = model.getLastEventOfGCID(context.get(GCID));
        }
        if (event == null) {
            // log may be incomplete
            return;
        }
        GCEvent phase = event.getLastPhaseOfType(phaseType);
        if (phase == null) {
            phase = new GCEvent();
            phase.setEventType(phaseType);
            phase.setGcid(context.get(GCID));
            phase.setStartTime(context.get(UPTIME));
            if (phaseType == G1_CONCURRENT_MARK_ABORT || phaseType == G1_CONCURRENT_MARK_RESET_FOR_OVERFLOW ||
                    phaseType == CMS_CONCURRENT_INTERRUPTED || phaseType == CMS_CONCURRENT_FAILURE) {
                phase.setDuration(0);
            }
            model.addPhase(event, phase);
        }
        parseCollectionAndDuration(phase, context, value);
    }

    //[0.502s][info   ][gc,task      ] GC(0) Using 8 workers of 8 for evacuation
    //[2.984s][info   ][gc,task       ] GC(1) Using 2 workers of 2 for marking
    private static boolean parseWorker(AbstractGCLogParser parser, ParseRuleContext context, String text) {
        GCModel model = parser.getModel();
        String[] parts = GCLogUtil.splitBySpace(text);
        if (parts.length >= 7 && "Using".equals(parts[0]) && "workers".equals(parts[2])) {
            if ("evacuation".equals(parts[6])) {
                model.setParallelThread(Integer.parseInt(parts[4]));
            } else if ("marking".equals(parts[6])) {
                model.setConcurrentThread(Integer.parseInt(parts[4]));
            }
            return true;
        }
        return false;
    }

    protected abstract GCEventType getGCEventType(String eventString);
}
