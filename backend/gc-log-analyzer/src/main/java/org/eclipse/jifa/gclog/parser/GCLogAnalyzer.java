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
import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.gclog.model.GCModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class GCLogAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GCLogAnalyzer.class);
    private File file;
    private ProgressListener listener;

    private final int MAX_SINGLE_LINE_LENGTH = 2048; // max length in hotspot

    public GCLogAnalyzer(File file, ProgressListener listener) {
        this.file = file;
        this.listener = listener;
    }

    public GCModel parse() throws Exception {
        try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr)) {
            listener.beginTask("Paring " + file.getName(), 1000);
            listener.sendUserMessage(ProgressListener.Level.INFO, "Reading gc log file.", null);
            GCLogParserFactory logParserFactory = new GCLogParserFactory();
            br.mark(GCLogParserFactory.MAX_ATTEMPT_LINE * MAX_SINGLE_LINE_LENGTH);
            GCLogParser parser = logParserFactory.getParser(br);
            br.reset();
            // first read original info from log file
            GCModel model = parser.parse(br);
            if (model.isEmpty()) {
                throw new JifaException("Fail to parse gclog. Is this really a gc log?\"");
            }
            // then calculate derived info for query from original info
            listener.worked(500);
            listener.sendUserMessage(ProgressListener.Level.INFO, "Calculating information from original data.", null);
            model.calculateDerivedInfo(listener);
            return model;
        } catch (Exception e) {
            LOGGER.info("fail to parse gclog {}: {}", file.getName(), e.getMessage());
            throw e;
        }
    }
}
