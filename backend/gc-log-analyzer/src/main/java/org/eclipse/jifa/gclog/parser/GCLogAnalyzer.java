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
import org.eclipse.jifa.common.listener.DefaultProgressListener;
import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.gclog.model.GCModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class GCLogAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GCLogAnalyzer.class);
    private File file;
    private ProgressListener listener;

    private final int MAX_SINGLE_LINE_LENGTH = 2048; // max length in hotspot

    public GCLogAnalyzer(File file, ProgressListener listener) {
        this.file = file;
        this.listener = listener;
    }

    public GCLogAnalyzer(File file) {
        this(file, new DefaultProgressListener());
    }

    public GCModel parse() throws Exception {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            listener.beginTask("Paring " + file.getName(), 1000);
            listener.sendUserMessage(ProgressListener.Level.INFO, "Deciding gc log format.", null);

            // decide log format
            GCLogParserFactory logParserFactory = new GCLogParserFactory();
            br.mark(GCLogParserFactory.MAX_ATTEMPT_LINE * MAX_SINGLE_LINE_LENGTH);
            GCLogParser parser = logParserFactory.getParser(br);
            listener.worked(100);

            try {
                br.reset();
            } catch (IOException e) {
                // Recreate stream in case mark invalid. This is unlikely but possible when the log
                // contains undesired characters
                br.close();
                br = new BufferedReader(new FileReader(file));
            }

            // read original info from log file
            listener.sendUserMessage(ProgressListener.Level.INFO, "Parsing gc log file.", null);
            GCModel model = parser.parse(br);
            if (model.isEmpty()) {
                throw new JifaException("Fail to parse gc log. Is this really a gc log?");
            }
            listener.worked(500);

            // calculate derived info for query from original info
            listener.sendUserMessage(ProgressListener.Level.INFO, "Calculating information from original data.", null);
            model.calculateDerivedInfo(listener);

            return model;
        } catch (Exception e) {
            LOGGER.info("fail to parse gclog {}: {}", file.getName(), e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }
}
