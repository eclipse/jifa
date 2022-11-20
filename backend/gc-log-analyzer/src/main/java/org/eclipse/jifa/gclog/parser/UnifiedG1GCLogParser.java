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

import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType;
import org.eclipse.jifa.gclog.model.G1GCModel;
import org.eclipse.jifa.gclog.model.GCEventType;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.util.Constant;
import org.eclipse.jifa.gclog.util.GCLogUtil;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.parser.ParseRule.*;
import static org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext.GCID;
import static org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext.UPTIME;

public class UnifiedG1GCLogParser extends UnifiedG1OrGenerationalGCLogParser {
    private final static GCEventType[] YOUNG_MIXED = {YOUNG_GC, G1_MIXED_GC};
    private final static GCEventType[] CONCURRENT_CYCLE_CPU_TIME_EVENTS = {
            YOUNG_GC, G1_MIXED_GC, FULL_GC, G1_PAUSE_CLEANUP, G1_REMARK};

    /*
     * [0.001s][warning][gc] -XX:+PrintGCDetails is deprecated. Will use -Xlog:gc* instead.
     * [0.004s][info   ][gc,heap] Heap region size: 1M
     * [0.019s][info   ][gc     ] Using G1
     * [0.019s][info   ][gc,heap,coops] Heap address: 0x0000000095c00000, size: 1700 MB, Compressed Oops mode: 32-bit
     * [0.050s][info   ][gc           ] Periodic GC disabled
     *
     * [0.751s][info][gc,start     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
     * [0.752s][info][gc,task      ] GC(0) Using 2 workers of 2 for evacuation
     * [0.760s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms
     * [0.354s][info][gc,phases   ] GC(0)   Merge Heap Roots: 0.1ms
     * [0.760s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 5.9ms
     * [0.760s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 2.3ms
     * [0.760s][info][gc,phases    ] GC(0)   Other: 0.3ms
     * [0.760s][info][gc,heap      ] GC(0) Eden regions: 6->0(5)
     * [0.760s][info][gc,heap      ] GC(0) Survivor regions: 0->1(1)
     * [0.760s][info][gc,heap      ] GC(0) Old regions: 0->0
     * [0.760s][info][gc,heap      ] GC(0) Humongous regions: 0->0
     * [0.760s][info][gc,metaspace ] GC(0) Metaspace: 10707K->10707K(1058816K)
     * [0.760s][info][gc           ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 96M->3M(2048M) 8.547ms
     * [0.760s][info][gc,cpu       ] GC(0) User=0.01s Sys=0.01s Real=0.01s
     *
     * [2.186s][info][gc           ] GC(5) Concurrent Cycle
     * [2.186s][info][gc           ] GC(5) Concurrent Mark Cycle
     * [2.186s][info][gc,marking   ] GC(5) Concurrent Clear Claimed Marks
     * [2.186s][info][gc,marking   ] GC(5) Concurrent Clear Claimed Marks 0.016ms
     * [2.186s][info][gc,marking   ] GC(5) Concurrent Scan Root Regions
     * [2.189s][info][gc,marking   ] GC(5) Concurrent Scan Root Regions 3.214ms
     * [2.189s][info][gc,marking   ] GC(5) Concurrent Mark (2.189s)
     * [2.189s][info][gc,marking   ] GC(5) Concurrent Mark Reset For Overflow
     * [2.189s][info][gc,marking   ] GC(5) Concurrent Mark From Roots
     * [2.190s][info][gc,task      ] GC(5) Using 2 workers of 2 for marking
     * [2.190s][info][gc,marking   ] GC(5) Concurrent Mark From Roots 0.226ms
     * [2.190s][info][gc,marking   ] GC(5) Concurrent Preclean
     * [2.190s][info][gc,marking   ] GC(5) Concurrent Preclean 0.030ms
     * [2.190s][info][gc,marking   ] GC(5) Concurrent Mark (2.189s, 2.190s) 0.272ms
     * [2.190s][info][gc,start     ] GC(5) Pause Remark
     * [2.193s][info][gc,stringtable] GC(5) Cleaned string and symbol table, strings: 10318 processed, 0 removed, symbols: 69242 processed, 330 removed
     * [2.193s][info][gc            ] GC(5) Pause Remark 14M->14M(2048M) 3.435ms
     * [2.193s][info][gc,cpu        ] GC(5) User=0.01s Sys=0.00s Real=0.00s
     * [2.193s][info][gc,marking    ] GC(5) Concurrent Rebuild Remembered Sets
     * [2.193s][info][gc,marking    ] GC(5) Concurrent Rebuild Remembered Sets 0.067ms
     * [2.194s][info][gc,start      ] GC(5) Pause Cleanup
     * [2.194s][info][gc            ] GC(5) Pause Cleanup 14M->14M(2048M) 0.067ms
     * [2.194s][info][gc,cpu        ] GC(5) User=0.00s Sys=0.00s Real=0.00s
     * [2.194s][info][gc,marking    ] GC(5) Concurrent Cleanup for Next Mark
     * [2.217s][info][gc,marking    ] GC(5) Concurrent Cleanup for Next Mark 23.105ms
     * [2.217s][info][gc            ] GC(5) Concurrent Cycle 30.799ms
     *
     * [6.845s][info][gc,task       ] GC(26) Using 2 workers of 2 for full compaction
     * [6.845s][info][gc,start      ] GC(26) Pause Full (System.gc())
     * [6.857s][info][gc,phases,start] GC(26) Phase 1: Mark live objects
     * [6.907s][info][gc,stringtable ] GC(26) Cleaned string and symbol table, strings: 11395 processed, 5 removed, symbols: 69956 processed, 0 removed
     * [6.907s][info][gc,phases      ] GC(26) Phase 1: Mark live objects 49.532ms
     * [6.907s][info][gc,phases,start] GC(26) Phase 2: Prepare for compaction
     * [6.922s][info][gc,phases      ] GC(26) Phase 2: Prepare for compaction 15.369ms
     * [6.922s][info][gc,phases,start] GC(26) Phase 3: Adjust pointers
     * [6.947s][info][gc,phases      ] GC(26) Phase 3: Adjust pointers 25.161ms
     * [6.947s][info][gc,phases,start] GC(26) Phase 4: Compact heap
     * [6.963s][info][gc,phases      ] GC(26) Phase 4: Compact heap 16.169ms
     * [6.966s][info][gc,heap        ] GC(26) Eden regions: 4->0(6)
     * [6.966s][info][gc,heap        ] GC(26) Survivor regions: 1->0(1)
     * [6.966s][info][gc,heap        ] GC(26) Old regions: 80->6
     * [6.966s][info][gc,heap        ] GC(26) Humongous regions: 2->2
     * [6.966s][info][gc,metaspace   ] GC(26) Metaspace: 22048K->22048K(1069056K)
     * [6.966s][info][gc             ] GC(26) Pause Full (System.gc()) 1368M->111M(2048M) 120.634ms
     * [6.966s][info][gc,cpu         ] GC(26) User=0.22s Sys=0.01s Real=0.12s
     *
     * [2.145s][info][gc          ] GC(3) Concurrent Undo Cycle
     * [2.145s][info][gc,marking  ] GC(3) Concurrent Cleanup for Next Mark
     * [2.145s][info][gc,marking  ] GC(3) Concurrent Cleanup for Next Mark 0.109ms
     * [2.145s][info][gc          ] GC(3) Concurrent Undo Cycle 0.125ms
     */
    private static List<ParseRule> withoutGCIDRules;
    private static List<ParseRule> withGCIDRules;

    static {
        initializeParseRules();
    }

    private static void initializeParseRules() {
        withoutGCIDRules = new ArrayList<>(getSharedWithoutGCIDRules());
        withoutGCIDRules.add(new PrefixAndValueParseRule("Heap region size", UnifiedG1GCLogParser::parseHeapRegionSize));
        withoutGCIDRules.add(new PrefixAndValueParseRule("Heap Region Size:", UnifiedG1GCLogParser::parseHeapRegionSize));

        withGCIDRules = new ArrayList<>(getSharedWithGCIDRules());
        withGCIDRules.add(new PrefixAndValueParseRule("  Pre Evacuate Collection Set", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("  Merge Heap Roots", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("  Evacuate Collection Set", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("  Post Evacuate Collection Set", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("  Other", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Cycle", UnifiedG1GCLogParser::parseConcurrentCycle));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Mark Cycle", UnifiedG1GCLogParser::parseConcurrentCycle));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Undo Cycle", UnifiedG1GCLogParser::parseConcurrentCycle));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Clear Claimed Marks", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Scan Root Regions", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Mark From Roots", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Mark", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Mark Reset For Overflow", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Preclean", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Pause Remark", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Rebuild Remembered Sets", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Pause Cleanup", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Cleanup for Next Mark", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Phase 1: Mark live objects", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Phase 2: Prepare for compaction", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Phase 3: Adjust pointers", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Phase 4: Compact heap", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new PrefixAndValueParseRule("Concurrent Mark Abort", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new FixedContentParseRule("To-space exhausted", UnifiedG1GCLogParser::parseToSpaceExhausted));
    }

    @Override
    protected List<ParseRule> getWithoutGCIDRules() {
        return withoutGCIDRules;
    }

    @Override
    protected List<ParseRule> getWithGCIDRules() {
        return withGCIDRules;
    }

    /*
     * [2.145s][info][gc          ] GC(3) Concurrent Undo Cycle
     * [2.186s][info][gc           ] GC(5) Concurrent Cycle
     * [2.186s][info][gc           ] GC(5) Concurrent Mark Cycle
     */
    private static void parseConcurrentCycle(AbstractGCLogParser parser, ParseRuleContext context, String prefix, String value) {
        GCModel model = parser.getModel();
        GCEventType eventType = "Concurrent Undo Cycle".equals(prefix) ? G1_CONCURRENT_UNDO_CYCLE : G1_CONCURRENT_CYCLE;
        boolean end = value.endsWith("ms");
        GCEvent event;
        if (!end || (event = model.getLastEventOfType(eventType)).getDuration() != Constant.UNKNOWN_DOUBLE) {
            event = new GCEvent();
            event.setStartTime(context.get(UPTIME));
            event.setEventType(eventType);
            event.setGcid(context.get(GCID));
            model.putEvent(event);
        }
        parseCollectionAndDuration(event, context, value);
    }

    private static void parseHeapRegionSize(AbstractGCLogParser parser, ParseRuleContext context, String prefix, String value) {
        G1GCModel model = (G1GCModel) parser.getModel();
        model.setHeapRegionSize(GCLogUtil.toByte(value));
        model.setRegionSizeExact(true);
    }

    private static void parseToSpaceExhausted(AbstractGCLogParser parser, ParseRuleContext context) {
        GCModel model = parser.getModel();
        GCEvent event = model.getLastEventOfType(YOUNG_MIXED);
        if (event == null) {
            // log may be incomplete
            return;
        }
        event.setTrue(GCEventBooleanType.TO_SPACE_EXHAUSTED);
    }

    @Override
    protected GCEvent getCPUTimeEventOrPhase(GCEvent event) {
        if (event.getEventType() == YOUNG_GC || event.getEventType() == FULL_GC || event.getEventType() == G1_MIXED_GC) {
            return event;
        } else if (event.getEventType() == G1_CONCURRENT_CYCLE) {
            return getModel().getLastEventOfType(CONCURRENT_CYCLE_CPU_TIME_EVENTS);
        } else {
            return null;
        }
    }

    @Override
    protected GCEventType getGCEventType(String eventString) {
        switch (eventString) {
            case "Pre Evacuate Collection Set":
                return G1_COLLECT_PRE_EVACUATION;
            case "Merge Heap Roots":
                return G1_MERGE_HEAP_ROOTS;
            case "Evacuate Collection Set":
                return G1_COLLECT_EVACUATION;
            case "Post Evacuate Collection Set":
                return G1_COLLECT_POST_EVACUATION;
            case "Other":
                return G1_COLLECT_OTHER;
            case "Concurrent Clear Claimed Marks":
                return G1_CONCURRENT_CLEAR_CLAIMED_MARKS;
            case "Concurrent Scan Root Regions":
                return G1_CONCURRENT_SCAN_ROOT_REGIONS;
            case "Concurrent Mark From Roots":
                return G1_CONCURRENT_MARK_FROM_ROOTS;
            case "Concurrent Mark":
                return G1_CONCURRENT_MARK;
            case "Concurrent Preclean":
                return G1_CONCURRENT_PRECLEAN;
            case "Pause Remark":
                return G1_REMARK;
            case "Concurrent Rebuild Remembered Sets":
                return G1_CONCURRENT_REBUILD_REMEMBERED_SETS;
            case "Pause Cleanup":
                return G1_PAUSE_CLEANUP;
            case "Concurrent Cleanup for Next Mark":
                return G1_CONCURRENT_CLEANUP_FOR_NEXT_MARK;
            case "Phase 1: Mark live objects":
                return G1_MARK_LIVE_OBJECTS;
            case "Phase 2: Prepare for compaction":
                return G1_PREPARE_FOR_COMPACTION;
            case "Phase 3: Adjust pointers":
                return G1_ADJUST_POINTERS;
            case "Phase 4: Compact heap":
                return G1_COMPACT_HEAP;
            case "Concurrent Mark Abort":
                return G1_CONCURRENT_MARK_ABORT;
            case "Concurrent Mark Reset For Overflow":
                return G1_CONCURRENT_MARK_RESET_FOR_OVERFLOW;
            default:
                ErrorUtil.shouldNotReachHere();
        }
        return null;
    }
}
