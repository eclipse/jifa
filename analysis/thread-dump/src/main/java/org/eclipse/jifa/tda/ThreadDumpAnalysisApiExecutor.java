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
package org.eclipse.jifa.tda;

import org.eclipse.jifa.analysis.AbstractApiExecutor;
import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Map;

public class ThreadDumpAnalysisApiExecutor extends AbstractApiExecutor<ThreadDumpAnalyzer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    protected ThreadDumpAnalyzer buildAnalyzer(Path target, Map<String, String> options, ProgressListener listener) {
        return new ThreadDumpAnalyzer(target, listener);
    }

    @Override
    public void clean(Path target) {
        super.clean(target);
        File kryo = target.resolveSibling(target.toFile().getName() + ".kryo").toFile();
        if (kryo.exists()) {
            if (!kryo.delete()) {
                LOGGER.warn("Failed to delete kryo file: {}", kryo.getAbsolutePath());
            }
        }
    }

    @Override
    public String namespace() {
        return "thread-dump";
    }
}
