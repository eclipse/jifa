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
import org.eclipse.jifa.gclog.model.GCEventType;
import org.eclipse.jifa.gclog.model.GCModel;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jifa.gclog.model.GCEventType.*;

public class UnifiedGenerationalGCLogParser extends UnifiedG1OrGenerationalGCLogParser {
    private final static GCEventType[] CMS_CPU_TIME_EVENTS = {CMS_INITIAL_MARK, CMS_CONCURRENT_MARK,
            CMS_CONCURRENT_PRECLEAN, CMS_CONCURRENT_ABORTABLE_PRECLEAN, CMS_FINAL_REMARK,
            CMS_CONCURRENT_SWEEP, CMS_CONCURRENT_RESET};

    /*
     * cms
     * [0.276s][info ][gc,start     ] GC(0) Pause Young (Allocation Failure)
     * [0.276s][info ][gc,task      ] GC(0) Using 8 workers of 8 for evacuation
     * [0.278s][debug][gc,age       ] GC(0) Desired survivor size 1114112 bytes, new threshold 1 (max threshold 6)
     * [0.278s][info ][gc,heap      ] GC(0) ParNew: 19277K->1969K(19648K)
     * [0.278s][info ][gc,heap      ] GC(0) CMS: 21564K->23127K(43712K)
     * [0.278s][info ][gc,metaspace ] GC(0) Metaspace: 5319K->5319K(1056768K)
     * [0.278s][info ][gc           ] GC(0) Pause Young (Allocation Failure) 39M->24M(61M) 2.065ms
     * [0.278s][info ][gc,cpu       ] GC(0) User=0.00s Sys=0.00s Real=0.00s
     * [1.017s][info ][gc,promotion ] Promotion failed
     *
     * [1.017s][info ][gc,start     ] GC(1) Pause Full (Allocation Failure)
     * [1.017s][info ][gc,phases,start] GC(1) Phase 1: Mark live objects
     * [1.019s][info ][gc,phases      ] GC(1) Phase 1: Mark live objects 1.788ms
     * [1.019s][info ][gc,phases,start] GC(1) Phase 2: Compute new object addresses
     * [1.020s][info ][gc,phases      ] GC(1) Phase 2: Compute new object addresses 0.485ms
     * [1.020s][info ][gc,phases,start] GC(1) Phase 3: Adjust pointers
     * [1.021s][info ][gc,phases      ] GC(1) Phase 3: Adjust pointers 1.138ms
     * [1.021s][info ][gc,phases,start] GC(1) Phase 4: Move objects
     * [1.022s][info ][gc,phases      ] GC(1) Phase 4: Move objects 0.903ms
     * [1.022s][info ][gc             ] GC(1) Pause Full (Allocation Failure) 60M->3M(61M) 4.853ms
     *
     * [2.278s][info ][gc,start     ] GC(2) Pause Initial Mark
     * [2.279s][info ][gc           ] GC(2) Pause Initial Mark 29M->29M(61M) 0.184ms
     * [2.279s][info ][gc,cpu       ] GC(2) User=0.00s Sys=0.00s Real=0.00s
     * [2.279s][info ][gc           ] GC(2) Concurrent Mark
     * [2.279s][info ][gc,task      ] GC(2) Using 2 workers of 2 for marking
     * [2.280s][info ][gc           ] GC(2) Concurrent Mark 1.553ms
     * [2.280s][info ][gc,cpu       ] GC(2) User=0.00s Sys=0.00s Real=0.00s
     * [2.282s][info ][gc           ] GC(2) Concurrent Preclean
     * [2.282s][info ][gc           ] GC(2) Concurrent Preclean 0.172ms
     * [2.282s][info ][gc,cpu       ] GC(2) User=0.00s Sys=0.00s Real=0.00s
     * [2.283s][info ][gc,start     ] GC(2) Pause Remark
     * [2.284s][info ][gc           ] GC(2) Pause Remark 26M->26M(61M) 1.736ms
     * [2.284s][info ][gc,cpu       ] GC(2) User=0.00s Sys=0.00s Real=0.01s
     * [2.284s][info ][gc           ] GC(2) Concurrent Sweep
     * [2.285s][info ][gc           ] GC(2) Concurrent Sweep 0.543ms
     * [2.285s][info ][gc,cpu       ] GC(2) User=0.00s Sys=0.00s Real=0.00s
     * [2.285s][info ][gc           ] GC(2) Concurrent Reset
     * [2.285s][info ][gc           ] GC(2) Concurrent Reset 0.323ms
     * [2.285s][info ][gc,cpu       ] GC(2) User=0.01s Sys=0.00s Real=0.00s
     * [2.285s][info ][gc,heap      ] GC(2) Old: 23127K->2019K(43712K)
     * ----------------------------------------------------------------------
     * parallel
     * [0.020s][info][gc,start     ] GC(0) Pause Young (Allocation Failure)
     * [0.026s][info ][gc,heap      ] GC(0) PSYoungGen: 16384K->2559K(18944K)
     * [0.026s][info ][gc,heap      ] GC(0) ParOldGen: 0K->2121K(44032K)
     * [0.026s][info ][gc,metaspace ] GC(0) Metaspace: 15746K->15746K(1062912K)
     * [0.026s][info ][gc           ] GC(0) Pause Young (Allocation Failure) 16M->4M(61M) 5.423ms
     * [0.026s][info ][gc,cpu       ] GC(0) User=0.02s Sys=0.01s Real=0.00s
     *
     * [1.115s][info ][gc,start     ] GC(1) Pause Full (Ergonomics)
     * [1.116s][info ][gc,phases,start] GC(1) Marking Phase
     * [1.120s][info ][gc,phases      ] GC(1) Marking Phase 4.518ms
     * [1.120s][info ][gc,phases,start] GC(1) Summary Phase
     * [1.120s][info ][gc,phases      ] GC(1) Summary Phase 0.013ms
     * [1.120s][info ][gc,phases,start] GC(1) Adjust Roots
     * [1.122s][info ][gc,phases      ] GC(1) Adjust Roots 2.423ms
     * [1.122s][info ][gc,phases,start] GC(1) Compaction Phase
     * [1.128s][info ][gc,phases      ] GC(1) Compaction Phase 5.461ms
     * [1.128s][info ][gc,phases,start] GC(1) Post Compact
     * [1.129s][info ][gc,phases      ] GC(1) Post Compact 0.974ms
     * [1.130s][info ][gc,heap        ] GC(1) PSYoungGen: 13467K->0K(16896K)
     * [1.130s][info ][gc,heap        ] GC(1) ParOldGen: 42920K->7823K(26624K)
     * [1.130s][info ][gc,metaspace   ] GC(1) Metaspace: 15855K->15855K(1064960K)
     * [1.130s][info ][gc             ] GC(1) Pause Full (Ergonomics) 55M->7M(42M) 14.092ms
     * [1.130s][info ][gc,cpu         ] GC(1) User=0.04s Sys=0.00s Real=0.02s
     * ----------------------------------------------------------------------
     * serial
     * [1.206s][info ][gc,start       ] GC(0) Pause Young (Allocation Failure)
     * [1.207s][info ][gc,heap        ] GC(0) DefNew: 17950K->1955K(19648K)
     * [1.207s][info ][gc,heap        ] GC(0) Tenured: 6759K->6759K(43712K)
     * [1.207s][info ][gc,metaspace   ] GC(0) Metaspace: 16398K->16398K(1064960K)
     * [1.207s][info ][gc             ] GC(0) Pause Young (Allocation Failure) 24M->8M(61M) 0.508ms
     * [1.207s][info ][gc,cpu         ] GC(0) User=0.00s Sys=0.00s Real=0.00s
     *
     * [2.401s][info ][gc,start       ] GC(1) Pause Full (Allocation Failure)
     * [2.401s][info ][gc,phases,start] GC(1) Phase 1: Mark live objects
     * [2.406s][info ][gc,phases      ] GC(1) Phase 1: Mark live objects 5.032ms
     * [2.406s][info ][gc,phases,start] GC(1) Phase 2: Compute new object addresses
     * [2.407s][info ][gc,phases      ] GC(1) Phase 2: Compute new object addresses 0.965ms
     * [2.407s][info ][gc,phases,start] GC(1) Phase 3: Adjust pointers
     * [2.412s][info ][gc,phases      ] GC(1) Phase 3: Adjust pointers 4.156ms
     * [2.412s][info ][gc,phases,start] GC(1) Phase 4: Move objects
     * [2.412s][info ][gc,phases      ] GC(1) Phase 4: Move objects 0.280ms
     * [2.412s][info ][gc             ] GC(1) Pause Full (Allocation Failure) 60M->6M(61M) 11.072ms
     */
    private static List<ParseRule> withoutGCIDRules;
    private static List<ParseRule> withGCIDRules;

    static {
        initializeParseRules();
    }

    private static void initializeParseRules() {
        withoutGCIDRules = new ArrayList<>(getSharedWithoutGCIDRules());

        withGCIDRules = new ArrayList<>(getSharedWithGCIDRules());
        withGCIDRules.add(new ParseRule.FixedContentParseRule("Promotion failed", UnifiedGenerationalGCLogParser::parsePromotionFailed));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Phase 1: Mark live objects", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Phase 2: Compute new object addresses", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Phase 3: Adjust pointers", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Phase 4: Move objects", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Pause Initial Mark", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Mark", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Preclean", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Abortable Preclean", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Pause Remark", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Sweep", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Concurrent Reset", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Marking Phase", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Summary Phase", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Adjust Roots", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Compaction Phase", UnifiedG1OrGenerationalGCLogParser::parsePhase));
        withGCIDRules.add(new ParseRule.PrefixAndValueParseRule("Post Compact", UnifiedG1OrGenerationalGCLogParser::parsePhase));
    }

    @Override
    protected List<ParseRule> getWithoutGCIDRules() {
        return withoutGCIDRules;
    }

    @Override
    protected List<ParseRule> getWithGCIDRules() {
        return withGCIDRules;
    }

    private static void parsePromotionFailed(AbstractGCLogParser parser, ParseRule.ParseRuleContext context) {
        GCModel model = parser.getModel();
        GCEvent event = model.getLastEventOfType(YOUNG_GC);
        if (event == null) {
            return;
        }
        event.setTrue(GCEventBooleanType.PROMOTION_FAILED);
    }

    @Override
    protected GCEvent getCPUTimeEventOrPhase(GCEvent event) {
        if (event.getEventType() == YOUNG_GC || event.getEventType() == FULL_GC) {
            return event;
        } else if (event.getEventType() == CMS_CONCURRENT_MARK_SWEPT) {
            return getModel().getLastEventOfType(CMS_CPU_TIME_EVENTS);
        } else {
            return null;
        }
    }

    @Override
    protected GCEventType getGCEventType(String eventString) {
        switch (eventString) {
            case "Phase 1: Mark live objects":
                return SERIAL_MARK_LIFE_OBJECTS;
            case "Phase 2: Compute new object addresses":
                return SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES;
            case "Phase 3: Adjust pointers":
                return SERIAL_ADJUST_POINTERS;
            case "Phase 4: Move objects":
                return SERIAL_MOVE_OBJECTS;
            case "Pause Initial Mark":
                return CMS_INITIAL_MARK;
            case "Concurrent Mark":
                return CMS_CONCURRENT_MARK;
            case "Concurrent Preclean":
                return CMS_CONCURRENT_PRECLEAN;
            case "Concurrent Abortable Preclean":
                return CMS_CONCURRENT_ABORTABLE_PRECLEAN;
            case "Pause Remark":
                return CMS_FINAL_REMARK;
            case "Concurrent Sweep":
                return CMS_CONCURRENT_SWEEP;
            case "Concurrent Reset":
                return CMS_CONCURRENT_RESET;
            case "Marking Phase":
                return PARALLEL_PHASE_MARKING;
            case "Summary Phase":
                return PARALLEL_PHASE_SUMMARY;
            case "Adjust Roots":
                return PARALLEL_PHASE_ADJUST_ROOTS;
            case "Compaction Phase":
                return PARALLEL_PHASE_COMPACTION;
            case "Post Compact":
                return PARALLEL_PHASE_POST_COMPACT;
            default:
                ErrorUtil.shouldNotReachHere();
        }
        return null;
    }
}
