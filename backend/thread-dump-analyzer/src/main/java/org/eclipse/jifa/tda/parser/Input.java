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

package org.eclipse.jifa.tda.parser;

import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;

public class Input implements Closeable {

    private final LineNumberReader lnr;

    private String current;

    public Input(Path dumpPath) throws IOException {
        lnr = new LineNumberReader(new FileReader(dumpPath.toFile()));
    }

    public void mark() throws IOException {
        lnr.mark(1024);
    }

    public void reset() throws IOException {
        lnr.reset();
    }

    public int lineNumber() {
        return lnr.getLineNumber();
    }

    public String readLine() throws IOException {
        current = lnr.readLine();
        if (current != null) {
            current = current.trim();
        }
        return current;
    }

    public String currentLine() {
        return current;
    }

    @Override
    public void close() throws IOException {
        lnr.close();
    }
}
