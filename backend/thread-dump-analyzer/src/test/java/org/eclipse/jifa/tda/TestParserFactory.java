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

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;

import org.eclipse.jifa.common.listener.ProgressListener;
import org.eclipse.jifa.tda.parser.ParserFactory;
import org.junit.Test;

public class TestParserFactory extends TestBase {
    
    @Test
    public void testBuildParser() throws Exception {
        Path resource = pathOfResource("jstack_8.log");
        assertEquals(10,ParserFactory.buildParser(resource).parse(resource, ProgressListener.NoOpProgressListener).getJavaThreads().size());

        resource = pathOfResource("jstack_11_with_deadlocks.log");
        assertEquals(11,ParserFactory.buildParser(resource).parse(resource, ProgressListener.NoOpProgressListener).getJavaThreads().size());

        resource = pathOfResource("thread_mx.log");
        assertEquals(2,ParserFactory.buildParser(resource).parse(resource, ProgressListener.NoOpProgressListener).getJavaThreads().size());
    }


}
