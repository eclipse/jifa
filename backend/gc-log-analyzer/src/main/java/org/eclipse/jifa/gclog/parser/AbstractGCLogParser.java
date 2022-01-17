/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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

import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.model.GCModelFactory;
import org.eclipse.jifa.gclog.parser.ParseRule.ParseRuleContext;
import org.eclipse.jifa.gclog.vo.GCLogMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.List;

public abstract class AbstractGCLogParser implements GCLogParser {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractGCLogParser.class);

    private GCModel model;
    private GCLogMetadata metadata;

    public GCLogMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(GCLogMetadata metadata) {
        this.metadata = metadata;
    }

    protected GCModel getModel() {
        return model;
    }

    // for the sake of performance, will try to use less regular expression
    public final GCModel parse(BufferedReader br) throws Exception {
        model = GCModelFactory.getModel(metadata.getCollector());
        model.setLogStyle(metadata.getStyle());
        String line;
        while ((line = br.readLine()) != null) {
            try {
                if (line.length() > 0) {
                    doParseLine(line);
                }
            } catch (Exception e) {
                LOGGER.debug("fail to parse \"{}\", {}", line, e.getMessage());
            }
        }
        try {
            endParsing();
        } catch (Exception e) {
            LOGGER.debug("fail to parse \"{}\", {}", line, e.getMessage());
        }

        return model;
    }

    protected abstract void doParseLine(String line);

    protected void endParsing() {
    }

    // return true if text can be parsed by any rule
    // order of rules matters
    protected boolean doParseUsingRules(AbstractGCLogParser parser, ParseRuleContext context, String text, List<ParseRule> rules) {
        for (ParseRule rule : rules) {
            if (rule.doParse(parser, context, text)) {
                return true;
            }
        }
        return false;
    }
}
