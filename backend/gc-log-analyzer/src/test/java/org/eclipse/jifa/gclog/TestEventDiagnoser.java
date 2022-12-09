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

package org.eclipse.jifa.gclog;

import org.eclipse.jifa.common.listener.DefaultProgressListener;
import org.eclipse.jifa.gclog.diagnoser.*;
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.model.GCEventType;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.parser.GCLogParser;
import org.eclipse.jifa.gclog.parser.GCLogParserFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.eclipse.jifa.gclog.TestUtil.stringToBufferedReader;
import static org.eclipse.jifa.gclog.diagnoser.AbnormalType.*;


public class TestEventDiagnoser {
    public static final double DELTA = 1e-6;

    @Before
    public void setExtendTime() {
        GlobalDiagnoser.setExtendTime(30000);
    }

    @Test
    public void testDiagnoseBasic() throws Exception {
        String log = "2022-09-05T17:30:59.396+0800: 25730.571: [GC pause (G1 Humongous Allocation) (young) (to-space exhausted), 0.6835408 secs]\n" +
                "   [Parallel Time: 213.6 ms, GC Workers: 4]\n" +
                "      [GC Worker Start (ms): Min: 25730571.4, Avg: 25730571.6, Max: 25730572.1, Diff: 0.7]\n" +
                "      [Ext Root Scanning (ms): Min: 5.9, Avg: 7.4, Max: 9.1, Diff: 3.2, Sum: 29.7]\n" +
                "      [Update RS (ms): Min: 6.7, Avg: 7.8, Max: 8.2, Diff: 1.5, Sum: 31.0]\n" +
                "         [Processed Buffers: Min: 22, Avg: 27.8, Max: 31, Diff: 9, Sum: 111]\n" +
                "      [Scan RS (ms): Min: 25.9, Avg: 26.0, Max: 26.3, Diff: 0.4, Sum: 104.2]\n" +
                "      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]\n" +
                "      [Object Copy (ms): Min: 171.8, Avg: 172.1, Max: 172.7, Diff: 0.9, Sum: 688.5]\n" +
                "      [Termination (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]\n" +
                "         [Termination Attempts: Min: 1, Avg: 1.0, Max: 1, Diff: 0, Sum: 4]\n" +
                "      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]\n" +
                "      [GC Worker Total (ms): Min: 212.9, Avg: 213.4, Max: 213.6, Diff: 0.7, Sum: 853.6]\n" +
                "      [GC Worker End (ms): Min: 25730785.0, Avg: 25730785.0, Max: 25730785.0, Diff: 0.0]\n" +
                "   [Code Root Fixup: 0.1 ms]\n" +
                "   [Code Root Purge: 0.0 ms]\n" +
                "   [Clear CT: 0.6 ms]\n" +
                "   [Other: 469.2 ms]\n" +
                "      [Evacuation Failure: 466.7 ms]\n" +
                "      [Choose CSet: 0.0 ms]\n" +
                "      [Ref Proc: 0.8 ms]\n" +
                "      [Ref Enq: 0.0 ms]\n" +
                "      [Redirty Cards: 0.6 ms]\n" +
                "      [Humongous Register: 0.0 ms]\n" +
                "      [Humongous Reclaim: 0.4 ms]\n" +
                "      [Free CSet: 0.2 ms]\n" +
                "   [Eden: 1824.0M(2368.0M)->0.0B(480.0M) Survivors: 98304.0K->32768.0K Heap: 4576.2M(5120.0M)->3681.4M(5120.0M)]\n" +
                " [Times: user=2.17 sys=0.08, real=0.69 secs] ";
        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        GCModel model = parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        GlobalDiagnoseInfo diagnose = model.getGlobalDiagnoseInfo(AnalysisConfig.defaultConfig(model));

        EventDiagnoseInfo[] eventDiagnoseInfos = diagnose.getEventDiagnoseInfos();
        Assert.assertEquals(eventDiagnoseInfos.length, model.getAllEvents().size());

        long hit = 0;
        for (GCEvent event : model.getAllEvents()) {
            GCEventType type = event.getEventType();
            EventAbnormalSet abs = diagnose.getEventDiagnoseInfo(event).getAbnormals();
            if (type == GCEventType.YOUNG_GC) {
                hit++;
                absContainExactTypes(abs, List.of(TO_SPACE_EXHAUSTED, BAD_DURATION, BAD_PROMOTION));

            } else if (type == GCEventType.G1_EVACUATION_FAILURE) {
                hit++;
                absContainExactTypes(abs, List.of(BAD_EVENT_TYPE, BAD_DURATION));
            } else {
                assert abs.isEmpty();
            }
        }
        Assert.assertEquals(hit, 2);
    }

    private void absContainExactTypes(EventAbnormalSet abs, List<AbnormalType> types) {
        Assert.assertEquals(abs.size(), types.size());
        for (AbnormalType type : types) {
            Assert.assertTrue(abs.contains(type));
        }
    }

}
