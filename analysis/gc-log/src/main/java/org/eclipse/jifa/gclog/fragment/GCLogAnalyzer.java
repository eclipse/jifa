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
package org.eclipse.jifa.gclog.fragment;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.parser.GCLogParser;
import org.eclipse.jifa.gclog.parser.GCLogParserFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
@Slf4j
public class GCLogAnalyzer {
    private final Map<Map<String, String>, GCLogParser> parserMap = new ConcurrentHashMap<>();

    public List<Metric> parseToMetrics(List<String> rawContext, Map<String, String> instanceId, long startTime, long endTime) throws Exception {
        Context context = new Context(rawContext);
        BufferedReader br = context.toBufferedReader();
        GCLogParser parser = selectParser(instanceId, br);
        GCModel model = parser.parse(br);
        br.close();
        if (!model.isEmpty()) {
            model.calculateDerivedInfo(null);
            return new GCModelConverter().toMetrics(model, instanceId, startTime, endTime);
        }
        return null;
    }

    public GCModel parseToGCModel(List<String> rawContext, Map<String, String> instanceId) {
        Context context = new Context(rawContext);
        BufferedReader br = context.toBufferedReader();
        GCModel model = null;
        try {
            GCLogParser parser = selectParser(instanceId, br);
            model = parser.parse(br);
            br.close();
            if (!model.isEmpty()) {
                model.calculateDerivedInfo(null);
            } else {
                model = null;
            }
        } catch (Exception e) {
            log.error("fail to parse context ");
            log.error(e.getMessage());
            model = null;
        } finally {
            return model;
        }
    }

    private GCLogParser selectParser(Map<String, String> instanceId, BufferedReader br) throws IOException {
        GCLogParser parser = parserMap.get(instanceId);
        if (parser == null) {
            GCLogParserFactory logParserFactory = new GCLogParserFactory();
            // max length in hotspot
            int MAX_SINGLE_LINE_LENGTH = 2048;
            br.mark(GCLogParserFactory.MAX_ATTEMPT_LINE * MAX_SINGLE_LINE_LENGTH);
            parser = logParserFactory.getParser(br);
            br.reset();
            parserMap.put(instanceId, parser);
        }
        return parser;
    }
}

