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
package org.eclipse.jifa.analysis;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.eclipse.jifa.analysis.annotation.ApiMeta;
import org.eclipse.jifa.analysis.listener.ProgressListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class TextAnalyzer {

    private final List<String> lines;

    public TextAnalyzer(Path path, ProgressListener listener) throws IOException {
        listener.beginTask("Parsing text", 100);
        String content = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
        listener.worked(80);
        lines = Lists.newArrayList(content.split("\n"));
        listener.worked(20);
    }

    @ApiMeta(aliases = "line")
    public String getLine(int lineNumber) {
        return lines.get(lineNumber - 1);
    }

    @ApiMeta(aliases = "totalLines")
    public long getTotalLines() {
        return lines.size();
    }
}
