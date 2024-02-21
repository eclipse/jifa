/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.jfr;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.analysis.AbstractApiExecutor;
import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.eclipse.jifa.analysis.support.MethodNameConverter;
import org.eclipse.jifa.jfr.api.JFRAnalyzer;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

@Slf4j
public class JFRAnalysisApiExecutor extends AbstractApiExecutor<JFRAnalyzer> {
    @Override
    public String namespace() {
        return "jfr-file";
    }

    @Override
    public Predicate<byte[]> matcher() {
        return new Predicate<>() {
            static final String HEADER = "FLR";

            @Override
            public boolean test(byte[] bytes) {
                return bytes.length > HEADER.length() && new String(bytes, 0, HEADER.length()).equals(HEADER);
            }
        };
    }

    @Override
    public boolean needOptionsForAnalysis(Path target) {
        return false;
    }

    @Override
    public void clean(Path target) {
        super.clean(target);
    }

    @Override
    protected MethodNameConverter methodNameConverter() {
        return MethodNameConverter.GETTER_METHOD;
    }

    @Override
    protected JFRAnalyzer buildAnalyzer(Path target, Map<String, String> options, ProgressListener listener) {
        return new JFRAnalyzerImpl(target, options, listener);
    }
}
