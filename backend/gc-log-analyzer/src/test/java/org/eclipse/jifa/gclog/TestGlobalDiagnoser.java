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
import org.eclipse.jifa.gclog.diagnoser.AnalysisConfig;
import org.eclipse.jifa.gclog.diagnoser.GlobalDiagnoser;
import org.eclipse.jifa.gclog.diagnoser.GlobalDiagnoser.*;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.parser.GCLogParser;
import org.eclipse.jifa.gclog.parser.GCLogParserFactory;
import org.eclipse.jifa.gclog.util.I18nStringView;
import org.eclipse.jifa.gclog.vo.TimeRange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

import static org.eclipse.jifa.gclog.TestUtil.*;
import static org.eclipse.jifa.gclog.diagnoser.AbnormalType.*;
import static org.eclipse.jifa.gclog.diagnoser.AnalysisConfig.defaultConfig;


public class TestGlobalDiagnoser {
    public static final double DELTA = 1e-6;

    @Before
    public void setExtendTime() {
        GlobalDiagnoser.setExtendTime(30000);
    }

    @Test
    public void testDiagnoseBasic() throws Exception {
        String log = "3.765: [Full GC (Metadata GC Threshold) 3.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]\n" +
                "12.765: [Full GC (Last ditch collection) 12.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]\n" +
                "80.765: [Full GC (System.gc()) 80.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]\n" +
                "95.765: [Full GC (Allocation Failure) 95.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]\n" +
                "103.765: [Full GC (Metadata GC Threshold) 103.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]\n" +
                "120.765: [Full GC (Metadata GC Threshold) 120.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]\n" +
                "155.765: [Full GC (Last ditch collection) 155.765: [CMS: 92159K->92159K(92160K), 0.1150376 secs] 99203K->99169K(101376K), [Metaspace: 3805K->3805K(1056768K)], 0.1150614 secs] [Times: user=0.11 sys=0.00, real=0.12 secs]";
        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        GCModel model = parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        Assert.assertEquals(model.getGcEvents().size(), 7);
        GlobalAbnormalInfo diagnose = new GlobalDiagnoser(model, defaultConfig(model)).diagnose();

        Map<String, List<Double>> seriousProblems = diagnose.getSeriousProblems();
        Assert.assertEquals(seriousProblems.size(), 3);
        Assert.assertArrayEquals(seriousProblems.get(METASPACE_FULL_GC.getName()).stream().mapToDouble(d -> d).toArray(),
                new double[]{3765, 12765, 103765, 120765, 155765}, DELTA);
        Assert.assertArrayEquals(seriousProblems.get(SYSTEM_GC.getName()).stream().mapToDouble(d -> d).toArray(),
                new double[]{80765}, DELTA);
        Assert.assertArrayEquals(seriousProblems.get(HEAP_MEMORY_FULL_GC.getName()).stream().mapToDouble(d -> d).toArray(),
                new double[]{95765}, DELTA);

        MostSeriousProblemSummary mostSeriousProblem = diagnose.getMostSeriousProblem();
        Assert.assertEquals(mostSeriousProblem.getProblem(), new I18nStringView("jifa.gclog.diagnose.abnormal.metaspaceFullGC"));
        Assert.assertTrue(mostSeriousProblem.getSuggestions().contains(new I18nStringView("jifa.gclog.diagnose.suggestion.checkMetaspace")));
        Assert.assertTrue(mostSeriousProblem.getSuggestions().contains(new I18nStringView("jifa.gclog.diagnose.suggestion.enlargeMetaspace")));
        Assert.assertFalse(mostSeriousProblem.getSuggestions().contains(new I18nStringView("jifa.gclog.diagnose.suggestion.upgradeTo11G1FullGC")));

        List<TimeRange> sites = mostSeriousProblem.getSites();
        Assert.assertEquals(sites.size(), 2);
        double[] actual = sitesToArray(sites);
        Assert.assertArrayEquals(actual, new double[]{0, 42765 + 115.0614, 73765, 155765 + 115.0614}, DELTA);

        AnalysisConfig config = defaultConfig(model);
        config.setTimeRange(new TimeRange(100000, 130000));
        diagnose = new GlobalDiagnoser(model, config).diagnose();
        seriousProblems = diagnose.getSeriousProblems();
        Assert.assertEquals(seriousProblems.size(), 1);
        Assert.assertEquals(seriousProblems.get(METASPACE_FULL_GC.getName()).size(), 2);
    }

    @Test
    public void testNoProblem() throws Exception {
        String log = "2020-12-27T00:29:54.757+0800: 1062298.547: [GC (Allocation Failure) 2020-12-27T00:29:54.757+0800: 1062298.547: [ParNew: 1826919K->78900K(1922432K), 0.0572643 secs] 3445819K->1697799K(4019584K), 0.0575802 secs] [Times: user=0.21 sys=0.00, real=0.05 secs]\n" +
                "2020-12-27T01:03:34.874+0800: 1064318.664: [GC (Allocation Failure) 2020-12-27T01:03:34.874+0800: 1064318.665: [ParNew: 1826612K->80229K(1922432K), 0.0554822 secs] 3445511K->1699129K(4019584K), 0.0558081 secs] [Times: user=0.21 sys=0.00, real=0.06 secs]\n" +
                "2020-12-27T01:47:52.957+0800: 1066976.747: [GC (Allocation Failure) 2020-12-27T01:47:52.957+0800: 1066976.747: [ParNew: 1827941K->134685K(1922432K), 0.0735485 secs] 3446841K->1753585K(4019584K), 0.0738704 secs] [Times: user=0.23 sys=0.00, real=0.07 secs]\n" +
                "2020-12-27T02:39:47.913+0800: 1070091.703: [GC (Allocation Failure) 2020-12-27T02:39:47.913+0800: 1070091.703: [ParNew: 1882397K->80118K(1922432K), 0.0804496 secs] 3501297K->1740153K(4019584K), 0.0807793 secs] [Times: user=0.26 sys=0.01, real=0.08 secs]\n" +
                "2020-12-27T03:31:13.977+0800: 1073177.767: [GC (Allocation Failure) 2020-12-27T03:31:13.977+0800: 1073177.767: [ParNew: 1827753K->74440K(1922432K), 0.0303335 secs] 3487788K->1734475K(4019584K), 0.0306656 secs] [Times: user=0.10 sys=0.00, real=0.03 secs]\n" +
                "2020-12-27T04:21:42.951+0800: 1076206.741: [GC (Allocation Failure) 2020-12-27T04:21:42.951+0800: 1076206.741: [ParNew: 1822152K->76115K(1922432K), 0.0357291 secs] 3482187K->1736150K(4019584K), 0.0360764 secs] [Times: user=0.13 sys=0.00, real=0.04 secs]\n" +
                "2020-12-27T05:13:19.851+0800: 1079303.642: [GC (Allocation Failure) 2020-12-27T05:13:19.851+0800: 1079303.642: [ParNew: 1823827K->68185K(1922432K), 0.0312874 secs] 3483862K->1728220K(4019584K), 0.0316223 secs] [Times: user=0.10 sys=0.01, real=0.03 secs]";
        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        GCModel model = parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        GlobalAbnormalInfo diagnose = new GlobalDiagnoser(model, defaultConfig(model)).diagnose();

        Assert.assertEquals(diagnose.getSeriousProblems().size(), 0);
        Assert.assertNull(diagnose.getMostSeriousProblem());
    }

    @Test
    public void testOrderSites() throws Exception {
        String log = "4.204: [GC (Allocation Failure) 4.204: [ParNew: 7575411K->562327K(7689600K), 0.4413861 secs] 8149808K->1188439K(20272512K), 0.4422627 secs] [Times: user=1.33 sys=0.07, real=0.44 secs]\n" +
                "24.204: [GC (Allocation Failure) 14.204: [ParNew: 7575411K->562327K(7689600K), 0.4413861 secs] 8149808K->1188439K(20272512K), 0.4422627 secs] [Times: user=1.33 sys=0.07, real=0.44 secs]\n" +
                "67.512: [GC (Allocation Failure) 67.512: [ParNew: 1879618K->93403K(1922432K), 0.1367250 secs] 1990018K->251801K(4019584K), 0.1369409 secs] [Times: user=0.40 sys=0.05, real=0.14 secs]\n" +
                "104.204: [GC (Allocation Failure) 104.204: [ParNew: 7575411K->562327K(7689600K), 0.4413861 secs] 8149808K->1188439K(20272512K), 0.4422627 secs] [Times: user=1.33 sys=0.07, real=0.44 secs]\n" +
                "114.204: [GC (Allocation Failure) 114.204: [ParNew: 7575411K->562327K(7689600K), 0.4413861 secs] 8149808K->1188439K(20272512K), 0.4422627 secs] [Times: user=1.33 sys=0.07, real=0.44 secs]\n" +
                "204.204: [GC (Allocation Failure) 204.204: [ParNew: 7575411K->562327K(7689600K), 0.4413861 secs] 8149808K->1188439K(20272512K), 0.4422627 secs] [Times: user=1.33 sys=0.07, real=0.44 secs]\n" +
                "214.512: [GC (Allocation Failure) 214.512: [ParNew: 1879618K->93403K(1922432K), 0.1367250 secs] 1990018K->251801K(4019584K), 0.1369409 secs] [Times: user=0.40 sys=0.05, real=0.14 secs]\n" +
                "224.204: [GC (Allocation Failure) 224.204: [ParNew: 7575411K->562327K(7689600K), 0.4413861 secs] 8149808K->1188439K(20272512K), 0.4422627 secs] [Times: user=1.33 sys=0.07, real=0.44 secs]\n" +
                "274.204: [GC (Allocation Failure) 274.204: [ParNew: 7575411K->562327K(7689600K), 0.4413861 secs] 8149808K->1188439K(20272512K), 0.4422627 secs] [Times: user=1.33 sys=0.07, real=0.44 secs]\n" +
                "404.204: [GC (Allocation Failure) 304.204: [ParNew: 7575411K->562327K(7689600K), 0.4413861 secs] 8149808K->1188439K(20272512K), 0.4422627 secs] [Times: user=1.33 sys=0.07, real=0.44 secs]\n" +
                "444.204: [GC (Allocation Failure) 344.204: [ParNew: 7575411K->562327K(7689600K), 0.4413861 secs] 8149808K->1188439K(20272512K), 0.4422627 secs] [Times: user=1.33 sys=0.07, real=0.44 secs]\n";
        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        GCModel model = parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        GlobalAbnormalInfo diagnose = new GlobalDiagnoser(model, defaultConfig(model)).diagnose();

        MostSeriousProblemSummary mostSeriousProblem = diagnose.getMostSeriousProblem();
        Assert.assertEquals(mostSeriousProblem.getProblem(), new I18nStringView("jifa.gclog.diagnose.abnormal.longYoungGCPause"));
        Assert.assertTrue(mostSeriousProblem.getSuggestions().contains(new I18nStringView("jifa.gclog.diagnose.suggestion.shrinkYoungGen")));

        List<TimeRange> sites = mostSeriousProblem.getSites();
        Assert.assertEquals(sites.size(), 3);
        double[] actual = sitesToArray(sites);
        Assert.assertArrayEquals(actual, new double[]{0, 54204 + 442.2627, 174204, 304204 + 442.2627, 374204, 444204 + 442.2627}, DELTA);
    }

    @Test
    public void testDiagnoseZGC() throws Exception {
        String log = "[7.000s] GC(374) Garbage Collection (Proactive)\n" +
                "[7.006s] GC(374) Pause Mark Start 4.459ms\n" +
                "[7.312s] GC(374) Concurrent Mark 306.720ms\n" +
                "[7.312s] GC(374) Pause Mark End 0.606ms\n" +
                "[7.313s] GC(374) Concurrent Process Non-Strong References 1.290ms\n" +
                "[7.314s] GC(374) Concurrent Reset Relocation Set 0.550ms\n" +
                "[7.314s] GC(374) Concurrent Destroy Detached Pages 0.001ms\n" +
                "[7.316s] GC(374) Concurrent Select Relocation Set 2.418ms\n" +
                "[7.321s] GC(374) Concurrent Prepare Relocation Set 5.719ms\n" +
                "[7.324s] GC(374) Pause Relocate Start 3.791ms\n" +
                "[7.356s] GC(374) Concurrent Relocate 32.974ms\n" +
                "[7.356s] GC(374) Load: 1.68/1.99/2.04\n" +
                "[7.356s] GC(374) MMU: 2ms/0.0%, 5ms/0.0%, 10ms/0.0%, 20ms/0.0%, 50ms/0.0%, 100ms/0.0%\n" +
                "[7.356s] GC(374) Mark: 8 stripe(s), 2 proactive flush(es), 1 terminate flush(es), 0 completion(s), 0 continuation(s)\n" +
                "[7.356s] GC(374) Relocation: Successful, 359M relocated\n" +
                "[7.356s] GC(374) NMethods: 21844 registered, 609 unregistered\n" +
                "[7.356s] GC(374) Metaspace: 125M used, 127M capacity, 128M committed, 130M reserved\n" +
                "[7.356s] GC(374) Soft: 18634 encountered, 0 discovered, 0 enqueued\n" +
                "[7.356s] GC(374) Weak: 56186 encountered, 18454 discovered, 3112 enqueued\n" +
                "[7.356s] GC(374) Final: 64 encountered, 16 discovered, 7 enqueued\n" +
                "[7.356s] GC(374) Phantom: 1882 encountered, 1585 discovered, 183 enqueued\n" +
                "[7.356s] GC(374)                Mark Start          Mark End        Relocate Start      Relocate End           High               Low\n" +
                "[7.356s] GC(374)  Capacity:    40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)      40960M (100%)\n" +
                "[7.356s] GC(374)   Reserve:       96M (0%)           96M (0%)           96M (0%)           96M (0%)           96M (0%)           96M (0%)\n" +
                "[7.356s] GC(374)      Free:    35250M (86%)       35210M (86%)       35964M (88%)       39410M (96%)       39410M (96%)       35210M (86%)\n" +
                "[7.356s] GC(374)      Used:     5614M (14%)        5654M (14%)        4900M (12%)        1454M (4%)         5654M (14%)        1454M (4%)\n" +
                "[7.356s] GC(374)      Live:         -              1173M (3%)         1173M (3%)         1173M (3%)             -                  -\n" +
                "[7.356s] GC(374) Allocated:         -                40M (0%)           40M (0%)          202M (0%)             -                  -\n" +
                "[7.356s] GC(374)   Garbage:         -              4440M (11%)        3686M (9%)          240M (1%)             -                  -\n" +
                "[7.356s] GC(374) Reclaimed:         -                  -               754M (2%)         4200M (10%)            -                  -\n" +
                "[7.356s] GC(374) Garbage Collection (Proactive) 5614M(14%)->1454M(4%)\n" +
                "[7.777s] Allocation Stall (ThreadPoolTaskScheduler-1) 0.204ms\n" +
                "[7.888s] Allocation Stall (NioProcessor-2) 0.391ms\n" +
                "[7.889s] Out Of Memory (thread 8)";

        GCLogParser parser = new GCLogParserFactory().getParser(stringToBufferedReader(log));
        GCModel model = parser.parse(stringToBufferedReader(log));
        model.calculateDerivedInfo(new DefaultProgressListener());
        GlobalAbnormalInfo diagnose = new GlobalDiagnoser(model, defaultConfig(model)).diagnose();

        Map<String, List<Double>> seriousProblems = diagnose.getSeriousProblems();
        Assert.assertEquals(seriousProblems.size(), 2);
        Assert.assertArrayEquals(seriousProblems.get(ALLOCATION_STALL.getName()).stream().mapToDouble(d -> d).toArray(),
                new double[]{7777 - 0.204, 7888 - 0.391}, DELTA);
        Assert.assertArrayEquals(seriousProblems.get(OUT_OF_MEMORY.getName()).stream().mapToDouble(d -> d).toArray(),
                new double[]{7889}, DELTA);

        MostSeriousProblemSummary mostSeriousProblem = diagnose.getMostSeriousProblem();
        Assert.assertEquals(mostSeriousProblem.getProblem(), new I18nStringView("jifa.gclog.diagnose.abnormal.outOfMemory"));
        Assert.assertTrue(mostSeriousProblem.getSuggestions().contains(new I18nStringView("jifa.gclog.diagnose.suggestion.checkMemoryLeak")));
        Assert.assertTrue(mostSeriousProblem.getSuggestions().contains(new I18nStringView("jifa.gclog.diagnose.suggestion.enlargeHeap")));

        List<TimeRange> sites = mostSeriousProblem.getSites();
        Assert.assertEquals(sites.size(), 1);
        double[] actual = sitesToArray(sites);
        Assert.assertArrayEquals(actual, new double[]{0, 7889}, DELTA);
    }

    private double[] sitesToArray(List<TimeRange> sites) {
        return sites.stream().flatMapToDouble(site -> DoubleStream.of(site.getStart(), site.getEnd())).toArray();
    }
}
