/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.gclog;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.analysis.listener.DefaultProgressListener;
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.event.eventInfo.MemoryArea;
import org.eclipse.jifa.gclog.fragment.GCLogAnalyzer;
import org.eclipse.jifa.gclog.model.*;
import org.eclipse.jifa.gclog.parser.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.eclipse.jifa.gclog.TestUtil.generateShuffledGCLog;
import static org.eclipse.jifa.gclog.event.eventInfo.MemoryArea.*;

/*
This test aims the same as TestFragmentedParserToGCModelLegacy.
Instead of checking each field with so many Assertions.assertEquals, this test barely compares both GC Models
produced by normal parser and fragmented parser. However, considering the large amount of fields in GC Event,
compareGCModel() only takes fields related with Metric into account.
Also, gclog of jdk11 and 17 and 8 with serialGC are excluded for the time being. Furthermore, by design,
IncompleteGCLog and 11_CMS gclogs are not supported.
 */

@Slf4j
public class TestFragmentedParserToGCModel {
    public static final double DELTA = 1e-6;

    @Test
    public void test() throws Exception {
        Set<String> gcLogSet = new ImmutableSet.Builder<String>().
                add("11G1Parser.log",
                        "8CMSParser.log", "8CMSPrintGC.log", "8G1PrintGC.log", "8ParallelGCParser.log", "8G1GCParser.log",
                        "8G1GCParserAdaptiveSize.log", "8ConcurrentPrintDateTimeStamp.log",
                        "8CMSCPUTime.log", "8CMSPromotionFailed.log",
                        "8CMSScavengeBeforeRemark.log", "8GenerationalGCInterleave.log").build();
        for (String gclog : gcLogSet) {
            compareGCModel(parseByNormalParser(gclog), parseByFragmentedParser(generateShuffledGCLog(gclog)));
        }
    }

    private void compareGCModel(GCModel left, GCModel right) {
        Assertions.assertEquals(left.getGcEvents().size(), right.getGcEvents().size());
        for (int i = 0; i < left.getGcEvents().size(); i++) {
            GCEvent l = left.getGcEvents().get(i), r = right.getGcEvents().get(i);
            Assertions.assertEquals(l.getGcid(), r.getGcid());
            Assertions.assertEquals(l.getStartTime(), r.getStartTime(), DELTA);
            Assertions.assertEquals(l.getDuration(), r.getDuration(), DELTA);
            Assertions.assertEquals(l.getPromotion(), r.getPromotion());
            Assertions.assertEquals(l.getPause(), r.getPause(), DELTA);
            Assertions.assertEquals(l.toString(), r.toString());
            compareCpuTime(l, r);
            compareCause(l, r);
            compareEventType(l, r);
            compareMemoryArea(l, r);
            compareSubPhase(l, r);
            compareReferenceGC(l, r);
        }
    }

    private void compareSubPhase(GCEvent l, GCEvent r) {
        if (l.getPhases() == null) {
            Assertions.assertNull(r.getPhases());
            return;
        }
        Assertions.assertEquals(l.getPhases().size(), r.getPhases().size());
        for (GCEvent gcEvent : l.getPhases()) {
            boolean find = false;
            for (GCEvent another : r.getPhases()) {
                if (gcEvent.getEventType().getName().equals(another.getEventType().getName())) {
                    find = true;
                    Assertions.assertEquals(gcEvent.getDuration(), another.getDuration(), DELTA);
                    break;
                }
            }
            Assertions.assertTrue(find);
        }
    }

    private void compareMemoryArea(GCEvent l, GCEvent r) {
        for (MemoryArea memoryArea : new MemoryArea[]{YOUNG, OLD, HEAP, METASPACE}) {
            if (l.getMemoryItem(memoryArea) == null) {
                Assertions.assertNull(r.getMemoryItem(memoryArea));
                continue;
            }
            Assertions.assertEquals(l.getMemoryItem(memoryArea), r.getMemoryItem(memoryArea));
        }
    }

    private void compareReferenceGC(GCEvent l, GCEvent r) {
        if (l.getReferenceGC() == null) {
            Assertions.assertNull(r.getReferenceGC());
            return;
        }
        Assertions.assertEquals(l.getReferenceGC().getSoftReferenceCount(), r.getReferenceGC().getSoftReferenceCount());
        Assertions.assertEquals(l.getReferenceGC().getSoftReferencePauseTime(), r.getReferenceGC().getSoftReferencePauseTime(), DELTA);
        Assertions.assertEquals(l.getReferenceGC().getWeakReferenceCount(), r.getReferenceGC().getWeakReferenceCount());
        Assertions.assertEquals(l.getReferenceGC().getWeakReferencePauseTime(), r.getReferenceGC().getWeakReferencePauseTime(), DELTA);
        Assertions.assertEquals(l.getReferenceGC().getFinalReferenceCount(), r.getReferenceGC().getFinalReferenceCount());
        Assertions.assertEquals(l.getReferenceGC().getFinalReferencePauseTime(), r.getReferenceGC().getFinalReferencePauseTime(), DELTA);
        Assertions.assertEquals(l.getReferenceGC().getPhantomReferenceCount(), r.getReferenceGC().getPhantomReferenceCount());
        Assertions.assertEquals(l.getReferenceGC().getPhantomReferencePauseTime(), r.getReferenceGC().getPhantomReferencePauseTime(), DELTA);
        Assertions.assertEquals(l.getReferenceGC().getPhantomReferenceFreedCount(), r.getReferenceGC().getPhantomReferenceFreedCount());
        Assertions.assertEquals(l.getReferenceGC().getJniWeakReferencePauseTime(), r.getReferenceGC().getJniWeakReferencePauseTime(), DELTA);
    }

    private void compareCpuTime(GCEvent l, GCEvent r) {
        if (l.getCpuTime() == null) {
            Assertions.assertNull(r.getCpuTime());
            return;
        }
        Assertions.assertEquals(l.getCpuTime().toString(), r.getCpuTime().toString());
    }

    private void compareCause(GCEvent l, GCEvent r) {
        if (l.getCause() == null) {
            Assertions.assertNull(r.getCause());
            return;
        }
        Assertions.assertEquals(l.getCause().getName(), r.getCause().getName());
    }

    private void compareEventType(GCEvent l, GCEvent r) {
        if (l.getEventType() == null) {
            Assertions.assertNull(r.getEventType());
            return;
        }
        Assertions.assertEquals(l.getEventType().getName(), r.getEventType().getName());
    }

    private GCModel parseByFragmentedParser(List<String> context) {
        return new GCLogAnalyzer().parseToGCModel(context, new HashMap<String, String>());
    }

    private GCModel parseByNormalParser(String name) throws Exception {
        GCLogParser gcLogParser = new GCLogParserFactory().getParser(TestUtil.getGCLog(name));
        GCModel model = gcLogParser.parse(TestUtil.getGCLog(name));
        model.calculateDerivedInfo(new DefaultProgressListener());
        return model;
    }
}
