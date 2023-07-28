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

import org.junit.jupiter.api.Test;

public class TestBaseAnalyzerExecutor {

    @Test
    public void test() throws Throwable {

//        ServiceLoader<ApiExecutor.Provider> loader = ServiceLoader
//                .load(ApiExecutor.Provider.class);
//
//        List<ApiExecutor.Provider> providers = new ArrayList<>();
//
//        loader.iterator().forEachRemaining(providers::add);
//
//        assertEquals(1, providers.size());
//
//        ApiExecutor.Provider provider = providers.get(0);
//
//        assertTrue(provider instanceof DummyAnalysisApiExecutor.Provider);
//
//        ApiExecutor executor = provider.getExecutor();
//
//        assertTrue(executor instanceof DummyAnalysisApiExecutor);
//
//        List<ApiMeta> supportedApis = executor.apis();
//
//        assertEquals(2 + 6, supportedApis.size());
//
//        ArgumentCarrier argumentCarrier = Mockito.mock(ArgumentCarrier.class);
//        Mockito.when(argumentCarrier.getString("file")).thenReturn("test.txt");
//        Mockito.when(argumentCarrier.getString("message")).thenReturn("0123456789");
//        Mockito.when(argumentCarrier.getString("arg0-key")).thenReturn("0123456789");
//
//        assertEquals("0123456789", executor.execute("dummy.echo", argumentCarrier));
//
//        executor.execute("dummy.send", argumentCarrier);
    }
}
