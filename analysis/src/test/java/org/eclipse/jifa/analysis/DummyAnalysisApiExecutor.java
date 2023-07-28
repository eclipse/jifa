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

import org.eclipse.jifa.analysis.listener.ProgressListener;

import java.nio.file.Path;
import java.util.Map;

public class DummyAnalysisApiExecutor extends AbstractApiExecutor<DummyAnalyzer> {

    @Override
    protected DummyAnalyzer buildAnalyzer(Path target, Map<String, String> options, ProgressListener listener) throws Throwable {
        return new DummyAnalyzer() {
            @Override
            public String echo(String message) {
                return message;
            }

            @Override
            public void send(String arg0) {
            }
        };
    }

    @Override
    public String namespace() {
        return "dummy";
    }
}
