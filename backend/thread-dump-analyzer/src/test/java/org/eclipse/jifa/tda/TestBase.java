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

package org.eclipse.jifa.tda;

import org.apache.commons.io.IOUtils;
import org.eclipse.jifa.tda.model.Snapshot;
import org.eclipse.jifa.tda.parser.JStackParser;
import org.eclipse.jifa.tda.parser.ParserException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.eclipse.jifa.common.listener.ProgressListener.NoOpProgressListener;

public class TestBase {
    protected final JStackParser analyzer = new JStackParser();

    protected Path pathOfResource(String name) throws URISyntaxException {
        return Paths.get(this.getClass().getClassLoader().getResource(name).toURI());
    }

    protected Path createTempFile(String content) throws IOException {
        Path path = Files.createTempFile("test", ".tmp");
        path.toFile().deleteOnExit();
        IOUtils.write(content, new FileOutputStream(path.toFile()), Charset.defaultCharset());
        return path;
    }

    protected Snapshot parseString(String content) throws ParserException, IOException {
        return analyzer.parse(createTempFile(content), NoOpProgressListener);
    }

    protected Snapshot parseFile(String name) throws ParserException, URISyntaxException {
        return analyzer.parse(pathOfResource(name), NoOpProgressListener);
    }
}
