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
import org.eclipse.jifa.gclog.event.evnetInfo.GCCause;
import org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea;
import org.eclipse.jifa.gclog.event.evnetInfo.GCMemoryItem;
import org.eclipse.jifa.gclog.event.evnetInfo.GCEventBooleanType;
import org.eclipse.jifa.gclog.model.GCEventType;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.util.GCLogUtil;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jifa.gclog.event.evnetInfo.MemoryArea.HEAP;
import static org.eclipse.jifa.gclog.model.GCEventType.*;
import static org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext;
import static org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext.EVENT;
import static org.eclipse.jifa.gclog.parser.ParseRule.PrefixAndValueParseRule;

public class PreUnifiedGenerationalGCLogParser extends AbstractPreUnifiedGCLogParser {
    private final static GCEventType[] YOUNG_FULL_GC = {YOUNG_GC, FULL_GC};
    private final static GCEventType[] REFERENCE_GC_TYPES = {YOUNG_GC, FULL_GC, WEAK_REFS_PROCESSING};
    private final static GCEventType[] CPU_TIME_TYPES = {YOUNG_GC, FULL_GC, CMS_INITIAL_MARK, CMS_CONCURRENT_MARK, CMS_CONCURRENT_PRECLEAN, CMS_CONCURRENT_ABORTABLE_PRECLEAN, CMS_FINAL_REMARK, CMS_CONCURRENT_SWEEP, CMS_CONCURRENT_RESET};
    private final static GCEventType[] CMS_FULL = {CMS_CONCURRENT_MARK_SWEPT, FULL_GC};

    /*
     * 2020-12-15T11:02:47.476+0800: 63471.266: [GC (Allocation Failure) 2020-12-15T11:02:47.476+0800: 63471.267: [ParNew: 1922374K->174720K(1922432K), 0.5852117 secs] 3476475K->1910594K(4019584K), 0.5855206 secs] [Times: user=1.69 sys=0.08, real=0.58 secs]
     * 2020-12-15T11:02:48.064+0800: 63471.854: [GC (CMS Initial Mark) [1 CMS-initial-mark: 1735874K(2097152K)] 1925568K(4019584K), 0.0760575 secs] [Times: user=0.17 sys=0.00, real=0.08 secs]
     * 2020-12-15T11:02:48.140+0800: 63471.930: [CMS-concurrent-mark-start]
     * 2020-12-15T11:02:48.613+0800: 63472.403: [CMS-concurrent-mark: 0.472/0.473 secs] [Times: user=0.68 sys=0.04, real=0.47 secs]
     * 2020-12-15T11:02:48.613+0800: 63472.403: [CMS-concurrent-preclean-start]
     * 2020-12-15T11:02:48.623+0800: 63472.413: [CMS-concurrent-preclean: 0.010/0.010 secs] [Times: user=0.00 sys=0.00, real=0.01 secs]
     * 2020-12-15T11:02:48.623+0800: 63472.413: [CMS-concurrent-abortable-preclean-start]
     *  CMS: abort preclean due to time 2020-12-15T11:02:54.732+0800: 63478.523: [CMS-concurrent-abortable-preclean: 6.102/6.109 secs] [Times: user=6.13 sys=0.09, real=6.11 secs]
     * 2020-12-15T11:02:54.734+0800: 63478.524: [GC (CMS Final Remark) [YG occupancy: 388009 K (1922432 K)]2020-12-15T11:02:54.734+0800: 63478.524: [Rescan (parallel) , 0.1034279 secs]
     * 2020-12-15T11:02:54.837+0800: 63478.627: [weak refs processing, 0.0063173 secs]
     * 2020-12-15T11:02:54.844+0800: 63478.634: [class unloading, 0.1256964 secs]
     * 2020-12-15T11:02:54.969+0800: 63478.760: [scrub symbol table, 0.0364073 secs]
     * 2020-12-15T11:02:55.006+0800: 63478.796: [scrub string table, 0.0056749 secs][1 CMS-remark: 1735874K(2097152K)] 2123883K(4019584K), 0.3066740 secs] [Times: user=0.54 sys=0.00, real=0.31 secs]
     * 2020-12-15T11:02:55.042+0800: 63478.833: [CMS-concurrent-sweep-start]
     * 2020-12-15T11:02:56.203+0800: 63479.993: [CMS-concurrent-sweep: 1.160/1.161 secs] [Times: user=1.23 sys=0.03, real=1.16 secs]
     * 2020-12-15T11:02:56.203+0800: 63479.993: [CMS-concurrent-reset-start]
     * 2020-12-15T11:02:56.218+0800: 63480.008: [CMS-concurrent-reset: 0.015/0.015 secs] [Times: user=0.00 sys=0.01, real=0.02 secs]
     * 2020-12-15T11:03:45.384+0800: 63529.175: [GC (Allocation Failure) 2020-12-15T11:03:45.385+0800: 63529.175: [ParNew: 1919804K->174720K(1922432K), 0.5404068 secs] 2298321K->734054K(4019584K), 0.5407823 secs] [Times: user=1.66 sys=0.00, real=0.54 secs]
     * 8910.956: [Full GC (Heap Dump Initiated GC) 8910.956: [CMS[YG occupancy: 1212954 K (1843200 K)]8911.637: [weak refs processing, 0.0018945 secs]8911.639: [class unloading, 0.0454119 secs]8911.684: [scrub symbol table, 0.0248340 secs]8911.709: [scrub string table, 0.0033967 secs]: 324459K->175339K(3072000K), 1.0268069 secs] 1537414K->1388294K(4915200K), [Metaspace: 114217K->113775K(1153024K)], 1.0277002 secs] [Times: user=1.71 sys=0.05, real=1.03 secs]
     * 2021-09-16T00:00:50.424+0800: 32628.004: [Full GC (GCLocker Initiated GC) 2021-09-16T00:00:50.424+0800: 32628.004: [CMS2021-09-16T00:00:52.070+0800: 32629.650: [CMS-concurrent-mark: 3.106/3.224 secs] [Times: user=17.06 sys=2.91, real=3.22 secs]
     * (concurrent mode failure): 4164260K->4013535K(4718592K), 16.9663174 secs] 9450852K->4013535K(10005184K), [Metaspace: 873770K->872947K(1869824K)], 16.9671173 secs] [Times: user=18.54 sys=0.08, real=16.97 secs]
     * 2016-03-22T10:02:41.962-0100: 13.396: [GC (Allocation Failure) 2016-03-22T10:02:41.962-0100: 13.396: [ParNew2016-03-22T10:02:41.970-0100: 13.404: [SoftReference, 0 refs, 0.0000260 secs]2016-03-22T10:02:41.970-0100: 13.404: [WeakReference, 59 refs, 0.0000110 secs]2016-03-22T10:02:41.970-0100: 13.404: [FinalReference, 1407 refs, 0.0025979 secs]2016-03-22T10:02:41.973-0100: 13.407: [PhantomReference, 0 refs, 0 refs, 0.0000131 secs]2016-03-22T10:02:41.973-0100: 13.407: [JNI Weak Reference, 0.0000088 secs]: 69952K->8704K(78656K), 0.0104509 secs] 69952K->11354K(253440K), 0.0105137 secs] [Times: user=0.04 sys=0.01, real=0.01 secs]
     * 2021-12-23T15:42:48.667+0800: [GC (CMS Final Remark) [YG occupancy: 10073 K (78656 K)]2021-12-23T15:42:48.667+0800: [Rescan (parallel) , 0.0020502 secs]2021-12-23T15:42:48.669+0800: [weak refs processing2021-12-23T15:42:48.669+0800: [SoftReference, 0 refs, 0.0000080 secs]2021-12-23T15:42:48.669+0800: [WeakReference, 0 refs, 0.0000067 secs]2021-12-23T15:42:48.669+0800: [FinalReference, 0 refs, 0.0000060 secs]2021-12-23T15:42:48.669+0800: [PhantomReference, 0 refs, 0 refs, 0.0000067 secs]2021-12-23T15:42:48.669+0800: [JNI Weak Reference, 0.0000164 secs], 0.0000611 secs]2021-12-23T15:42:48.669+0800: [class unloading, 0.0002626 secs]2021-12-23T15:42:48.669+0800: [scrub symbol table, 0.0003540 secs]2021-12-23T15:42:48.669+0800: [scrub string table, 0.0001338 secs][1 CMS-remark: 143081K(174784K)] 153154K(253440K), 0.0029093 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]
     * */

    private static List<ParseRule> fullSentenceRules;
    private static List<ParseRule> gcTraceTimeRules;

    static {
        initializeParseRules();
    }

    private static void initializeParseRules() {
        fullSentenceRules = new ArrayList<>();
        fullSentenceRules.add(commandLineRule);
        fullSentenceRules.add(cpuTimeRule);
        fullSentenceRules.add(new PrefixAndValueParseRule(" (concurrent mode interrupted)", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule(" (concurrent mode failure)", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        fullSentenceRules.add(new PrefixAndValueParseRule(" (promotion failed", PreUnifiedGenerationalGCLogParser::parsePromotionFailed));

        gcTraceTimeRules = new ArrayList<>();
        gcTraceTimeRules.add(PreUnifiedGenerationalGCLogParser::parseGenerationCollection);
        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC (CMS Initial Mark)", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("CMS-concurrent-mark", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("CMS-concurrent-preclean", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("CMS-concurrent-abortable-preclean", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC (CMS Final Remark)", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("Rescan", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("weak refs processing", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("class unloading", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("scrub symbol table", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("scrub string table", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("CMS-concurrent-sweep", PreUnifiedGenerationalGCLogParser::parseCMSPhase));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("CMS-concurrent-reset", PreUnifiedGenerationalGCLogParser::parseCMSPhase));

        gcTraceTimeRules.add(new PrefixAndValueParseRule("GC", PreUnifiedGenerationalGCLogParser::parseYoungFullGC));
        gcTraceTimeRules.add(new PrefixAndValueParseRule("Full GC", PreUnifiedGenerationalGCLogParser::parseYoungFullGC));
    }

    private static void parsePromotionFailed(AbstractGCLogParser parser, ParseRuleContext context, String s, String s1) {
        GCModel model = parser.getModel();
        GCEvent event = model.getLastEventOfType(YOUNG_FULL_GC);
        if (event == null) {
            return;
        }
        event.setTrue(GCEventBooleanType.PROMOTION_FAILED);
    }

    private static void parseCMSPhase(AbstractGCLogParser parser, ParseRuleContext context, String phaseName, String value) {
        GCModel model = parser.getModel();
        if (phaseName.startsWith("GC (")) {
            // "GC (CMS Final Remark)"
            phaseName = phaseName.substring(4, phaseName.length() - 1);
        } else if (phaseName.startsWith(" (")) {
            // " (concurrent mode interrupted)"
            phaseName = phaseName.substring(2, phaseName.length() - 1);
        }
        GCEventType phaseType = getGCEventType(phaseName);

        GCEvent phase = context.get(EVENT);
        if (phase == null) {
            // " (concurrent mode interrupted)"
            phase = new GCEvent();
            phase.setEventType(phaseType);
            GCEvent gc = model.getLastEventOfType(YOUNG_FULL_GC);
            if (gc == null) {
                return;
            }
            phase.setStartTime(gc.getStartTime());
            phase.setDuration(0);
        }

        GCEvent parent;
        if (phaseType == CMS_INITIAL_MARK) {
            parent = new GCEvent();
            parent.setEventType(CMS_CONCURRENT_MARK_SWEPT);
            parent.setStartTime(phase.getStartTime());
            model.putEvent(parent);
        } else {
            if (phaseType == WEAK_REFS_PROCESSING || phaseType == CMS_CLASS_UNLOADING ||
                    phaseType == CMS_SCRUB_STRING_TABLE || phaseType == CMS_SCRUB_SYMBOL_TABLE) {
                parent = model.getLastEventOfType(CMS_FULL);
            } else {
                parent = model.getLastEventOfType(CMS_CONCURRENT_MARK_SWEPT);
            }
            if (parent == null) {
                return;
            }
        }

        if (phaseType == CMS_FINAL_REMARK && parent.getLastPhaseOfType(CMS_FINAL_REMARK) != null) {
            // When we see the second cms final remark, this is actually the CMSScavengeBeforeRemark.
            // In current implementation, young gc must be put at the first level of event structure
            // otherwise it can not be correctly aggregated
            phase.setEventType(YOUNG_GC);
            phase.setCause(GCCause.CMS_FINAL_REMARK);
            phase.setTrue(GCEventBooleanType.IGNORE_PAUSE);
            model.putEvent(phase);
            return;
        }

        GCEvent phaseStart = parent.getLastPhaseOfType(phaseType);
        if (phaseStart == null) {
            phase.setEventType(phaseType);
            model.addPhase(parent, phase);
            if (phaseType == CMS_CONCURRENT_INTERRUPTED || phaseType == CMS_CONCURRENT_FAILURE) {
                phase.setDuration(0);
            }
        } else {
            copyPhaseDataToStart(phaseStart, phase);
        }
    }

    private static void parseYoungFullGC(AbstractGCLogParser parser, ParseRuleContext context, String prefix, String cause) {
        GCEventType eventType = prefix.equals("GC") ? YOUNG_GC : FULL_GC;
        GCEvent event = context.get(EVENT);
        event.setEventType(eventType);
        String[] causes = GCLogUtil.splitByBracket(cause);
        if (causes.length > 0) {
            event.setCause(causes[0]);
        }
        parser.getModel().putEvent(event);
    }

    private static boolean parseGenerationCollection(AbstractGCLogParser parser, ParseRuleContext context, String s) {
        MemoryArea area = MemoryArea.getMemoryArea(s);
        if (area == null) {
            return false;
        }
        // put the collection on the correct event
        GCModel model = parser.getModel();
        GCEvent event = model.getLastEventOfType(YOUNG_FULL_GC);
        if (event == null) {
            return true;
        }
        GCMemoryItem collection = ((GCEvent) context.get(EVENT)).getMemoryItem(HEAP);
        if (collection == null) {
            return true;
        }
        collection.setArea(area);
        event.setMemoryItem(collection);
        return true;
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

    @Override
    protected GCEvent getReferenceGCEvent() {
        return getModel().getLastEventOfType(REFERENCE_GC_TYPES);
    }

    @Override
    protected GCEventType[] getCPUTimeGCEvent() {
        return CPU_TIME_TYPES;
    }

    private static GCEventType getGCEventType(String eventString) {
        switch (eventString) {
            case "concurrent mode interrupted":
                return CMS_CONCURRENT_INTERRUPTED;
            case "concurrent mode failure":
                return CMS_CONCURRENT_FAILURE;
            case "CMS Initial Mark":
                return CMS_INITIAL_MARK;
            case "CMS-concurrent-mark":
                return CMS_CONCURRENT_MARK;
            case "CMS-concurrent-preclean":
                return CMS_CONCURRENT_PRECLEAN;
            case "CMS-concurrent-abortable-preclean":
                return CMS_CONCURRENT_ABORTABLE_PRECLEAN;
            case "CMS Final Remark":
                return CMS_FINAL_REMARK;
            case "Rescan":
                return CMS_RESCAN;
            case "weak refs processing":
                return WEAK_REFS_PROCESSING;
            case "class unloading":
                return CMS_CLASS_UNLOADING;
            case "scrub symbol table":
                return CMS_SCRUB_SYMBOL_TABLE;
            case "scrub string table":
                return CMS_SCRUB_STRING_TABLE;
            case "CMS-concurrent-sweep":
                return CMS_CONCURRENT_SWEEP;
            case "CMS-concurrent-reset":
                return CMS_CONCURRENT_RESET;
            default:
                ErrorUtil.shouldNotReachHere();
        }
        return null;
    }
}
