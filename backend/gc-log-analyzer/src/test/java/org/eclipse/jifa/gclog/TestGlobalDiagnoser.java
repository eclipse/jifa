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
import org.eclipse.jifa.gclog.diagnoser.GlobalDiagnoser;
import org.eclipse.jifa.gclog.diagnoser.GlobalDiagnoser.*;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.parser.GCLogParser;
import org.eclipse.jifa.gclog.parser.GCLogParserFactory;
import org.eclipse.jifa.gclog.util.I18nStringView;
import org.eclipse.jifa.gclog.vo.TimeRange;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

import static org.eclipse.jifa.gclog.TestUtil.*;
import static org.eclipse.jifa.gclog.diagnoser.AbnormalType.*;


public class TestGlobalDiagnoser {
    public static final double DELTA = 1e-6;

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

        List<TimeRange> sites = mostSeriousProblem.getSites();
        Assert.assertEquals(sites.size(), 2);
        double[] expectedSites = sitesToArray(sites);
        Assert.assertArrayEquals(expectedSites, new double[]{3765, 12765 + 115.0614, 103765, 155765 + 115.0614}, DELTA);
    }

    private double[] sitesToArray(List<TimeRange> sites) {
        return sites.stream().flatMapToDouble(site -> DoubleStream.of(site.getStart(), site.getEnd())).toArray();
    }
}
