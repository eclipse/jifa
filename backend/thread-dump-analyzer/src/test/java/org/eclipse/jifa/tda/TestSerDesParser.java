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

import org.eclipse.jifa.tda.model.Snapshot;
import org.eclipse.jifa.tda.parser.ParserException;
import org.eclipse.jifa.tda.parser.SerDesParser;
import org.junit.Assert;
import org.junit.Test;

import java.net.URISyntaxException;

import static org.eclipse.jifa.common.listener.ProgressListener.NoOpProgressListener;

public class TestSerDesParser extends TestBase {

    @Test
    public void test() throws ParserException, URISyntaxException {
        SerDesParser serDesAnalyzer = new SerDesParser(analyzer);
        Snapshot first = serDesAnalyzer.parse(pathOfResource("jstack_8.log"), NoOpProgressListener);
        Snapshot second = serDesAnalyzer.parse(pathOfResource("jstack_8.log"), NoOpProgressListener);
        Assert.assertEquals(first,  second);
    }
}
