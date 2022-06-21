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

import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.gclog.vo.GCCollectorType;
import org.eclipse.jifa.gclog.vo.GCLogParsingMetadata;
import org.eclipse.jifa.gclog.vo.GCLogStyle;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.BufferedReader;

import static org.eclipse.jifa.gclog.vo.GCLogStyle.*;
import static org.eclipse.jifa.gclog.vo.GCCollectorType.*;

public class GCLogParserFactory {
    static final int MAX_ATTEMPT_LINE = 1000;

    private static final ParserMetadataRule[] rules = {
            // style
            new ParserMetadataRule("[Times:", JDK8_STYLE, GCCollectorType.UNKNOWN),
            new ParserMetadataRule(": [GC", JDK8_STYLE, GCCollectorType.UNKNOWN),
            new ParserMetadataRule("[info]", UNIFIED_STYLE, GCCollectorType.UNKNOWN),
            new ParserMetadataRule("[gc]", UNIFIED_STYLE, GCCollectorType.UNKNOWN),

            // collector
            new ParserMetadataRule("PSYoungGen", GCLogStyle.UNKNOWN, PARALLEL),
            new ParserMetadataRule("DefNew", GCLogStyle.UNKNOWN, SERIAL),
            new ParserMetadataRule("ParNew", GCLogStyle.UNKNOWN, CMS),
            new ParserMetadataRule("Pre Evacuate Collection Set", UNIFIED_STYLE, G1),
            new ParserMetadataRule("G1 Evacuation Pause", GCLogStyle.UNKNOWN, G1),
            new ParserMetadataRule("[GC Worker Start (ms): ", GCLogStyle.UNKNOWN, G1),
            new ParserMetadataRule("Concurrent Reset Relocation Set", UNIFIED_STYLE, ZGC),
            new ParserMetadataRule("=== Garbage Collection Statistics ===", UNIFIED_STYLE, ZGC),
            new ParserMetadataRule("Pause Init Update Refs", UNIFIED_STYLE, SHENANDOAH),
            new ParserMetadataRule("Using Concurrent Mark Sweep", UNIFIED_STYLE, CMS),
            new ParserMetadataRule("Using G1", UNIFIED_STYLE, G1),
            new ParserMetadataRule("Using Parallel", UNIFIED_STYLE, PARALLEL),
            new ParserMetadataRule("Using Serial", UNIFIED_STYLE, SERIAL),
            new ParserMetadataRule("Using Shenandoah", UNIFIED_STYLE, SHENANDOAH),
            new ParserMetadataRule("Using The Z Garbage Collector", UNIFIED_STYLE, ZGC),
    };

    public GCLogParser getParser(BufferedReader br) {
        GCLogParsingMetadata metadata = getMetadata(br);
        return createParser(metadata);
    }

    private GCLogParsingMetadata getMetadata(BufferedReader br) {
        GCLogParsingMetadata result = new GCLogParsingMetadata(GCCollectorType.UNKNOWN, GCLogStyle.UNKNOWN);
        try {
            complete:
            for (int i = 0; i < MAX_ATTEMPT_LINE; i++) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                for (ParserMetadataRule rule : rules) {
                    if (!line.contains(rule.getText())) {
                        continue;
                    }
                    if (result.getStyle() == GCLogStyle.UNKNOWN) {
                        result.setStyle(rule.getStyle());
                    }
                    if (result.getCollector() == GCCollectorType.UNKNOWN) {
                        result.setCollector(rule.getCollector());
                    }
                    if (result.getCollector() != GCCollectorType.UNKNOWN && result.getStyle() != GCLogStyle.UNKNOWN) {
                        break complete;
                    }
                }
            }
        } catch (Exception e) {
            // do nothing, hopefully we have got enough information
        }
        return result;
    }

    private GCLogParser createParser(GCLogParsingMetadata metadata) {
        AbstractGCLogParser parser = null;
        if (metadata.getStyle() == JDK8_STYLE) {
            switch (metadata.getCollector()) {
                case SERIAL:
                case PARALLEL:
                case CMS:
                case UNKNOWN:
                    parser = new JDK8GenerationalGCLogParser();
                    break;
                case G1:
                    parser = new JDK8G1GCLogParser();
                    break;
                default:
                    ErrorUtil.shouldNotReachHere();
            }
        } else if (metadata.getStyle() == UNIFIED_STYLE) {
            switch (metadata.getCollector()) {
                case SERIAL:
                case PARALLEL:
                case CMS:
                case UNKNOWN:
                    parser = new JDK11GenerationalGCLogParser();
                    break;
                case G1:
                    parser = new JDK11G1GCLogParser();
                    break;
                case ZGC:
                    parser = new JDK11ZGCLogParser();
                    break;
                case SHENANDOAH:
                    throw new JifaException("Shenandoah is not supported.");
                default:
                    ErrorUtil.shouldNotReachHere();
            }
        } else {
            throw new JifaException("Can not recognize format. Is this really a gc log?");
        }
        parser.setMetadata(metadata);
        return parser;
    }

    @Data
    @AllArgsConstructor
    private static class ParserMetadataRule {
        private String text;
        private GCLogStyle style;
        private GCCollectorType collector;
    }
}
