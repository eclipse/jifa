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
package org.eclipse.jifa.profile.lang.java;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.analysis.listener.ProgressListener;
import org.eclipse.jifa.profile.ProfileAnalysisApiExecutor;
import org.eclipse.jifa.profile.api.ProfileAnalyzer;
import org.eclipse.jifa.profile.enums.Language;
import org.eclipse.jifa.profile.lang.java.common.ProfileDimension;
import org.eclipse.jifa.profile.vo.BasicMetadata;
import org.eclipse.jifa.profile.vo.FlameGraph;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.jifa.profile.lang.java.TestJFRAnalyzer.createTmpFileForResource;

@Slf4j
public class TestJavaProfileAnalyzerImpl {

    private static ProfileAnalyzer ANALYZER;

    @BeforeAll
    public static void init() throws Exception {
        Path path = createTmpFileForResource("jfr.jfr");
        Method buildAnalyzer = ProfileAnalysisApiExecutor.class.getDeclaredMethod("buildAnalyzer", Path.class, Map.class, ProgressListener.class);
        buildAnalyzer.setAccessible(true);
        Map<String, String> options = new HashMap<>();
        options.put("lang", "java");
        ANALYZER = (ProfileAnalyzer) buildAnalyzer.invoke(new ProfileAnalysisApiExecutor(), path, options, ProgressListener.NoOpProgressListener);
    }

    @AfterAll
    public static void clean() {
    }

    @Test
    public void testBasicMetadata() {
        BasicMetadata meta = ANALYZER.basic();
        Assertions.assertEquals(Language.JAVA, meta.getLanguage());
        Assertions.assertNotNull(meta.getPerfDimensions());
        Assertions.assertTrue(meta.getPerfDimensions().length > 0);
    }

    @Test
    public void testFlameGraph() {
        FlameGraph fg = ANALYZER.getFlameGraph(ProfileDimension.CPU.getKey(), false, null);
        Assertions.assertNotNull(fg.getData());
        Assertions.assertNotNull(fg.getSymbolTable());
        Assertions.assertNotNull(fg.getThreadSplit());
    }
}
