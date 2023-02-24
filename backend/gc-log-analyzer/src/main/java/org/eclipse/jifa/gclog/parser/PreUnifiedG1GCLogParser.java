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
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea;
import org.eclipse.jifa.gclog.event.evnetInfo.GCMemoryItem;
import org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType;
import org.eclipse.jifa.gclog.model.GCEventType;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.util.GCLogUtil;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.parser.ParseRule.*;
import static org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext.EVENT;

public class PreUnifiedG1GCLogParser extends AbstractPreUnifiedGCLogParser {
    private final static GCEventType[] REF_GC_TYPES = {YOUNG_GC, FULL_GC, G1_MIXED_GC, G1_REMARK};
    private final static GCEventType[] YOUNG_MIXED = {YOUNG_GC, G1_MIXED_GC};
    private final static GCEventType[] YOUNG_MIXED_FULL = {YOUNG_GC, G1_MIXED_GC, FULL_GC};
    private final static List<GCEventType> CPU_TIME_TYPES = List.of(YOUNG_GC, FULL_GC, G1_MIXED_GC, G1_REMARK, G1_PAUSE_CLEANUP);

    /*
     * 2021-05-19T22:52:16.311+0800: 3.960: [GC pause (G1 Evacuation Pause) (young)2021-05-19T22:52:16.351+0800: 4.000: [SoftReference, 0 refs, 0.0000435 secs]2021-05-19T22:52:16.352+0800: 4.000: [WeakReference, 374 refs, 0.0002082 secs]2021-05-19T22:52:16.352+0800: 4.001: [FinalReference, 5466 refs, 0.0141707 secs]2021-05-19T22:52:16.366+0800: 4.015: [PhantomReference, 0 refs, 0 refs, 0.0000253 secs]2021-05-19T22:52:16.366+0800: 4.015: [JNI Weak Reference, 0.0000057 secs], 0.0563085 secs]
     *   [Parallel Time: 39.7 ms, GC Workers: 4]
     *      [GC Worker Start (ms): Min: 3959.8, Avg: 3959.9, Max: 3960.1, Diff: 0.2]
     *      [Ext Root Scanning (ms): Min: 2.6, Avg: 10.1, Max: 17.9, Diff: 15.2, Sum: 40.4]
     *      [Update RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
     *         [Processed Buffers: Min: 0, Avg: 0.0, Max: 0, Diff: 0, Sum: 0]
     *      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
     *      [Code Root Scanning (ms): Min: 0.0, Avg: 0.5, Max: 2.1, Diff: 2.1, Sum: 2.1]
     *      [Object Copy (ms): Min: 18.1, Avg: 26.2, Max: 33.7, Diff: 15.6, Sum: 104.9]
     *      [Termination (ms): Min: 0.0, Avg: 1.5, Max: 3.5, Diff: 3.5, Sum: 6.2]
     *         [Termination Attempts: Min: 1, Avg: 21.8, Max: 51, Diff: 50, Sum: 87]
     *      [GC Worker Other (ms): Min: 0.0, Avg: 0.1, Max: 0.1, Diff: 0.0, Sum: 0.2]
     *      [GC Worker Total (ms): Min: 38.0, Avg: 38.5, Max: 39.5, Diff: 1.5, Sum: 153.8]
     *      [GC Worker End (ms): Min: 3998.0, Avg: 3998.4, Max: 3999.4, Diff: 1.4]
     *   [Code Root Fixup: 0.2 ms]
     *   [Code Root Purge: 0.2 ms]
     *   [Clear CT: 0.2 ms]
     *   [Other: 16.0 ms]
     *      [Evacuation Failure: 629.5 ms]
     *      [Choose CSet: 0.0 ms]
     *      [Ref Proc: 15.1 ms]
     *      [Ref Enq: 0.2 ms]
     *      [Redirty Cards: 0.1 ms]
     *      [Humongous Register: 0.0 ms]
     *      [Humongous Reclaim: 0.0 ms]
     *      [Free CSet: 0.3 ms]
     *   [Eden: 184.0M(184.0M)->0.0B(160.0M) Survivors: 0.0B->24.0M Heap: 184.0M(3800.0M)->19.3M(3800.0M)]
     *  [Times: user=0.07 sys=0.01, real=0.06 secs]
     * 2021-09-25T08:01:03.268+0800: 78304.230: [GC concurrent-root-region-scan-start]
     * 2021-09-25T08:01:03.429+0800: 78304.391: [GC concurrent-root-region-scan-end, 0.1608430 secs]
     * 2021-09-25T08:01:03.429+0800: 78304.391: [GC concurrent-mark-start]
     * 2021-09-25T08:01:06.138+0800: 78307.101: [GC concurrent-mark-reset-for-overflow]
     * 2021-08-25T11:28:23.995+0800: 114394.984: [GC concurrent-mark-abort]
     * 2021-09-25T08:01:18.109+0800: 78319.072: [GC concurrent-mark-end, 14.6803750 secs]
     * 2021-09-25T08:01:18.115+0800: 78319.078: [GC remark 2021-09-25T08:01:18.115+0800: 78319.078: [Finalize Marking, 0.1774665 secs] 2021-09-25T08:01:18.293+0800: 78319.255: [GC ref-proc, 0.1648116 secs] 2021-09-25T08:01:18.457+0800: 78319.420: [Unloading, 0.1221964 secs], 0.4785858 secs]
     *  [Times: user=1.47 sys=0.31, real=0.48 secs]
     * 2021-09-25T08:01:18.601+0800: 78319.563: [GC cleanup 11G->9863M(20G), 0.0659638 secs]
     *  [Times: user=0.20 sys=0.01, real=0.07 secs]
     * 2021-09-25T08:01:18.667+0800: 78319.630: [GC concurrent-cleanup-start]
     * 2021-09-25T08:01:18.668+0800: 78319.631: [GC concurrent-cleanup-end, 0.0010377 secs]
     * 2021-05-25T22:41:05.357+0800: 190.521: [Full GC (Allocation Failure)  2691M->476M(2724M), 1.9820132 secs]
     *   [Eden: 0.0B(1024.0M)->0.0B(1024.0M) Survivors: 0.0B->0.0B Heap: 2691.5M(2724.0M)->476.6M(2724.0M)], [Metaspace: 31984K->31982K(1079296K)]
     * [Times: user=2.64 sys=0.17, real=1.98 secs]
     * 2021-08-26T15:47:09.545+0800: 414187.453: [GC pause (G1 Evacuation Pause) (young)2021-08-26T15:47:09.611+0800: 414187.519: [SoftReference, 0 refs, 0.0000435 secs]2021-08-26T15:47:09.611+0800: 414187.519: [WeakReference, 0 refs, 0.0000062 secs]2021-08-26T15:47:09.611+0800: 414187.519: [FinalReference, 0 refs, 0.0000052 secs]2021-08-26T15:47:09.611+0800: 414187.519: [PhantomReference, 0 refs, 0 refs, 0.0000056 secs]2021-08-26T15:47:09.611+0800: 414187.519: [JNI Weak Reference, 0.0000129 secs] (to-space exhausted), 0.1011364 secs]
     * 2021-08-26T15:47:01.710+0800: 414179.619: [GC pause (G1 Evacuation Pause) (mixed)2021-08-26T15:47:01.727+0800: 414179.636: [SoftReference, 0 refs, 0.0000415 secs]2021-08-26T15:47:01.727+0800: 414179.636: [WeakReference, 0 refs, 0.0000061 secs]2021-08-26T15:47:01.727+0800: 414179.636: [FinalReference, 0 refs, 0.0000049 secs]2021-08-26T15:47:01.727+0800: 414179.636: [PhantomReference, 0 refs, 0 refs, 0.0000052 secs]2021-08-26T15:47:01.727+0800: 414179.636: [JNI Weak Reference, 0.0000117 secs] (to-space exhausted), 0.0264971 secs]
     * 2021-08-26T15:27:21.061+0800: 243.497: [GC remark 2021-08-26T15:27:21.061+0800: 243.497: [Finalize Marking, 0.0008929 secs] 2021-08-26T15:27:21.062+0800: 243.497: [GC ref-proc2021-08-26T15:27:21.062+0800: 243.497: [SoftReference, 0 refs, 0.0000304 secs]2021-08-26T15:27:21.062+0800: 243.498: [WeakReference, 560 refs, 0.0001581 secs]2021-08-26T15:27:21.062+0800: 243.498: [FinalReference, 22 refs, 0.0000966 secs]2021-08-26T15:27:21.062+0800: 243.498: [PhantomReference, 0 refs, 416 refs, 0.0001386 secs]2021-08-26T15:27:21.062+0800: 243.498: [JNI Weak Reference, 0.0000471 secs], 0.0005083 secs] 2021-08-26T15:27:21.062+0800: 243.498: [Unloading, 0.0150698 secs], 0.0414751 secs]
     */

    private static List<ParseRule> fullSentenceRules;
    private static List<ParseRule> gcTraceTimeRules;

    static {
        initializeParseRules();
    }

    private static void initializeParseRules() {
        fullSentenceRules = new ArrayList<>();
        fullSentenceRules.add(commandLineRule);
        fullSentenceRules.add(cpuTimeRule);
        fullSentenceRules.add(new FixedContentParseRule(" (to-space exhausted)", PreUnifiedG1GCLogParser::parseToSpaceExhausted));
        fullSentenceRules.add(new FixedContentParseRule("--", PreUnifiedG1GCLogParser::parseToSpaceExhausted));
        fullSentenceRules.add(new PrefixAndValueParseRule("   [Eden", PreUnifiedG1GCLogParser::parseMemoryChange));
        fullSentenceRules.add(new PrefixAndValueParseRule("   [Parallel Time", PreUnifiedG1GCLogParser::parseParallelWorker));
        // some phases are ignored
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Ext Root Scanning", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Update RS", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Scan RS", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Code Root Scanning", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Object Copy", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Termination", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("   [Code Root Fixup", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("   [Code Root Purge", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("   [Clear CT", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Evacuation Failure", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Choose CSet", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Ref Proc", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Ref Enq", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Redirty Cards", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Humongous Register", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Humongous Reclaim", PreUnifiedG1GCLogParser::parseYoungGCPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule("      [Free CSet", PreUnifiedG1GCLogParser::parseYoungGCPhase));

        gcTraceTimeRules = new ArrayList<>();
        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC concurrent-root-region-scan", PreUnifiedG1GCLogParser::parseConcurrentCyclePhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC concurrent-mark-reset-for-overflow", PreUnifiedG1GCLogParser::parseConcurrentCyclePhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC concurrent-mark-abort", PreUnifiedG1GCLogParser::parseConcurrentCyclePhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC concurrent-mark", PreUnifiedG1GCLogParser::parseConcurrentCyclePhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC remark", PreUnifiedG1GCLogParser::parseConcurrentCyclePhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("Finalize Marking", PreUnifiedG1GCLogParser::parseConcurrentCyclePhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC ref-proc", PreUnifiedG1GCLogParser::parseConcurrentCyclePhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("Unloading", PreUnifiedG1GCLogParser::parseConcurrentCyclePhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC cleanup", PreUnifiedG1GCLogParser::parseConcurrentCyclePhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC concurrent-cleanup", PreUnifiedG1GCLogParser::parseConcurrentCyclePhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC pause", PreUnifiedG1GCLogParser::parseYoungMixedFullGC));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("Full GC", PreUnifiedG1GCLogParser::parseYoungMixedFullGC));
    }

    private static void parseParallelWorker(AbstractGCLogParser parser, ParseRuleContext context, String prefix, String value) {
        int worker = Integer.parseInt(value.substring(value.lastIndexOf(' ') + 1, value.length() - 1));
        parser.getModel().setParallelThread(worker);
    }

    private static void parseConcurrentCyclePhase(AbstractGCLogParser parser, ParseRuleContext context, String phaseName, String value) {
        GCModel model = parser.getModel();
        GCEventType phaseType = getGCEventType(phaseName);

        GCEvent phase = context.get(EVENT);

        GCEvent parent;
        if (phaseType == G1_CONCURRENT_SCAN_ROOT_REGIONS && "-start".equals(value)) {
            parent = new GCEvent();
            parent.setEventType(G1_CONCURRENT_CYCLE);
            parent.setStartTime(phase.getStartTime());
            model.putEvent(parent);
        } else {
            parent = model.getLastEventOfType(G1_CONCURRENT_CYCLE);
        }
        GCEvent phaseStart = parent.getLastPhaseOfType(phaseType);
        if (phaseStart == null) {
            phase.setEventType(phaseType);
            model.addPhase(parent, phase);
            if (phaseType == G1_CONCURRENT_MARK_RESET_FOR_OVERFLOW || phaseType == G1_CONCURRENT_MARK_ABORT) {
                phase.setDuration(0);
            }
            ((AbstractPreUnifiedGCLogParser) parser).pushIfWaitingForCpuTime(phase);
        } else {
            copyPhaseDataToStart(phaseStart, phase);
            ((AbstractPreUnifiedGCLogParser) parser).pushIfWaitingForCpuTime(phaseStart);
        }
    }

    // "   [Eden: 0.0B(1760.0M)->0.0B(2304.0M) Survivors: 544.0M->0.0B Heap: 7521.7M(46144.0M)->7002.8M(46144.0M)], [Metaspace: 1792694K->291615K(698368K)]"
    private static void parseMemoryChange(AbstractGCLogParser parser, ParseRuleContext context, String prefix, String value) {
        GCEvent event = parser.getModel().getLastEventOfType(YOUNG_MIXED_FULL);
        if (event == null) {
            return;
        }
        String[] parts = GCLogUtil.splitBySpace("  [Eden: " + value);
        for (int i = 0; i < parts.length; i += 2) {
            String areaString = StringUtils.strip(parts[i], "[:");
            MemoryArea area = MemoryArea.getMemoryArea(areaString);
            String memoryChangeString = StringUtils.strip(parts[i + 1], ",]");
            long[] memories = GCLogUtil.parseMemorySizeFromTo(memoryChangeString);
            GCMemoryItem item = new GCMemoryItem(area, memories);
            event.setMemoryItem(item, true);
        }
    }

    private static void parseToSpaceExhausted(AbstractGCLogParser parser, ParseRuleContext context) {
        GCEvent event = parser.getModel().getLastEventOfType(YOUNG_MIXED);
        if (event == null) {
            return;
        }
        event.setTrue(GCEventBooleanType.TO_SPACE_EXHAUSTED);
    }

    @Override
    protected void doParseFullSentence(String sentence) {
        doParseUsingRules(this, new ParseRuleContext(), sentence, fullSentenceRules);
    }

    @Override
    protected void doParseGCTraceTime(GCEvent event, String title) {
        ParseRuleContext context = new ParseRuleContext();
        context.put(EVENT, event);
        doParseUsingRules(this, context, title, gcTraceTimeRules);
    }

    //     [Ext Root Scanning (ms): Min: 2.6, Avg: 10.1, Max: 17.9, Diff: 15.2, Sum: 40.4]
    //   [Code Root Fixup: 0.2 ms]
    private static void parseYoungGCPhase(AbstractGCLogParser parser, ParseRuleContext context, String prefix, String value) {
        GCEvent parent = parser.getModel().getLastEventOfType(YOUNG_MIXED);
        if (parent == null) {
            return;
        }
        GCEvent phase = new GCEvent();
        // no way to know it exactly, just copy parent's
        phase.setStartTime(parent.getStartTime());

        String phaseName = prefix.substring(prefix.indexOf('[') + 1);
        phase.setEventType(getGCEventType(phaseName));

        int durationIndexBegin, durationIndexEnd;
        if (value.endsWith(" ms]")) {
            durationIndexEnd = value.length() - " ms]".length();
            durationIndexBegin = value.lastIndexOf(' ', durationIndexEnd - 1) + 1;
        } else {
            durationIndexBegin = value.indexOf("Avg: ") + "Avg: ".length();
            durationIndexEnd = value.indexOf(',', durationIndexBegin);
        }
        double duration = Double.parseDouble(value.substring(durationIndexBegin, durationIndexEnd));
        phase.setDuration(duration);
        parser.getModel().addPhase(parent, phase);
    }

    private static void parseYoungMixedFullGC(AbstractGCLogParser parser, ParseRuleContext context, String prefix, String value) {
        GCEventType eventType = prefix.equals("GC pause") ? YOUNG_GC : FULL_GC;
        GCEvent event = context.get(EVENT);

        String[] causes = GCLogUtil.splitByBracket(value);
        for (int i = 0; i < causes.length; i++) {
            String cause = causes[i];
            // to space exhausted is not considered here because it is not printed together with other brackets
            if (cause.equals("mixed")) {
                eventType = G1_MIXED_GC;
            } else if (cause.equals("young")) {
                eventType = YOUNG_GC;
            } else if (cause.equals("initial-mark")) {
                event.setTrue(GCEventBooleanType.INITIAL_MARK);
            } else if (i == 0) {
                event.setCause(cause);
            }
        }
        event.setEventType(eventType);
        parser.getModel().putEvent(event);
        ((AbstractPreUnifiedGCLogParser) parser).pushIfWaitingForCpuTime(event);
    }

    @Override
    protected GCEvent getReferenceGCEvent() {
        return getModel().getLastEventOfType(REF_GC_TYPES);
    }

    @Override
    protected List<GCEventType> getCPUTimeGCEvent() {
        return CPU_TIME_TYPES;
    }

    private static GCEventType getGCEventType(String eventString) {
        switch (eventString) {
            case "Ext Root Scanning":
                return G1_EXT_ROOT_SCANNING;
            case "Update RS":
                return G1_UPDATE_RS;
            case "Scan RS":
                return G1_SCAN_RS;
            case "Code Root Scanning":
                return G1_CODE_ROOT_SCANNING;
            case "Object Copy":
                return G1_OBJECT_COPY;
            case "Termination":
                return G1_TERMINATION;
            case "Code Root Fixup":
                return G1_CODE_ROOT_FIXUP;
            case "Code Root Purge":
                return G1_CODE_ROOT_PURGE;
            case "Clear CT":
                return G1_CLEAR_CT;
            case "Evacuation Failure":
                return G1_EVACUATION_FAILURE;
            case "Choose CSet":
                return G1_CHOOSE_CSET;
            case "Ref Proc":
            case "GC ref-proc":
                return G1_GC_REFPROC;
            case "Ref Enq":
                return G1_REF_ENQ;
            case "Redirty Cards":
                return G1_REDIRTY_CARDS;
            case "Humongous Register":
                return G1_HUMONGOUS_REGISTER;
            case "Humongous Reclaim":
                return G1_HUMONGOUS_RECLAIM;
            case "Free CSet":
                return G1_FREE_CSET;
            case "GC concurrent-root-region-scan":
                return G1_CONCURRENT_SCAN_ROOT_REGIONS;
            case "GC concurrent-mark":
                return G1_CONCURRENT_MARK;
            case "GC concurrent-mark-reset-for-overflow":
                return G1_CONCURRENT_MARK_RESET_FOR_OVERFLOW;
            case "GC concurrent-mark-abort":
                return G1_CONCURRENT_MARK_ABORT;
            case "GC remark":
                return G1_REMARK;
            case "Finalize Marking":
                return G1_FINALIZE_MARKING;
            case "Unloading":
                return G1_UNLOADING;
            case "GC cleanup":
                return G1_PAUSE_CLEANUP;
            case "GC concurrent-cleanup":
                return G1_CONCURRENT_CLEANUP_FOR_NEXT_MARK;
            default:
                ErrorUtil.shouldNotReachHere();
        }
        return null;
    }
}
