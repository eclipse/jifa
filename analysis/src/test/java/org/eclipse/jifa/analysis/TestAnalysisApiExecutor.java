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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnalysisApiExecutor {

    @Test
    public void test() throws IOException, ExecutionException, InterruptedException {
        ApiService service = ApiService.getInstance();
        assertNotNull(service);
        Map<String, Set<Api>> apis = service.supportedApis();
        assertTrue(apis.containsKey("text"));
        assertEquals(1, apis.get("text").stream().filter(api -> api.name().equals("getLine")).count());
        assertEquals(1, apis.get("text").stream().filter(api -> api.name().equals("getTotalLines")).count());

        File tempFile = File.createTempFile("test", "txt");
        tempFile.deleteOnExit();

        FileUtils.writeStringToFile(tempFile, """
                                            Hello World
                                            Hello Jifa""",
                                    StandardCharsets.UTF_8);

        CompletableFuture<?> result = service.execute(tempFile.toPath(), "text", "analyze", new Object[]{tempFile.toPath(), null});
        assertNull(result.get());

        result = service.execute(tempFile.toPath(), "text", "progressOfAnalysis", new Object[]{tempFile.toPath()});
        Progress progress = (Progress) result.get();
        assertNotNull(progress);

        result = service.execute(tempFile.toPath(), "text", "getLine", new Object[]{1});
        String line = (String) result.get();
        assertEquals("Hello World", line);
        result = service.execute(tempFile.toPath(), "text", "getLine", new Object[]{2});
        line = (String) result.get();
        assertEquals("Hello Jifa", line);
    }
}
