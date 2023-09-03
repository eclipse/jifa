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

import java.net.URISyntaxException;

import org.eclipse.jifa.tda.model.Snapshot;
import org.eclipse.jifa.tda.parser.ParserException;
import org.eclipse.jifa.tda.parser.ThreadMXBeanParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestThreadMXBeanParser extends TestBase {

    @Before
    public void setup() {
        analyzer = new ThreadMXBeanParser();
    }

    @Test
    public void testCanParse() throws ParserException, URISyntaxException {
        Assert.assertFalse(analyzer.canParse(pathOfResource("jstack_8.log")));
        Assert.assertFalse(analyzer.canParse(pathOfResource("jstack_11_large_with_blocked.log")));
        Assert.assertFalse(analyzer.canParse(pathOfResource("jstack_11_with_deadlocks.log")));
        Assert.assertFalse(analyzer.canParse(pathOfResource("jstack_17_log_with_pid.log")));

        Assert.assertTrue(analyzer.canParse(pathOfResource("thread_mx.log")));
        Assert.assertTrue(analyzer.canParse(pathOfResource("thread_mx_karaf.log")));
    }

    @Test
    public void testThreadMXLog() throws ParserException, URISyntaxException {
        Snapshot snapshot = parseFile("thread_mx.log");
        Assert.assertTrue(snapshot.getErrors().isEmpty());
        Assert.assertEquals(1689678597166l, snapshot.getTimestamp());
        Assert.assertEquals(78789, snapshot.getPid());
        Assert.assertEquals(2, snapshot.getJavaThreads().size());
        Assert.assertEquals(
                "Thread-5980 (HornetQ-remoting-threads-HornetQServerImpl::serverUUID=a866a1bd-a922-11e7-8b97-0050569e60a9-2139288264-2029178161)",
                snapshot.getJavaThreads().get(0).getName());
    }

    @Test
    public void testThreadMXLogKaraf() throws ParserException, URISyntaxException {
        Snapshot snapshot = parseFile("thread_mx_karaf.log");
        Assert.assertTrue(snapshot.getErrors().isEmpty());
        Assert.assertEquals(-1, snapshot.getTimestamp());
        Assert.assertEquals(-1, snapshot.getPid());
        Assert.assertEquals(2, snapshot.getJavaThreads().size());
        Assert.assertEquals(
                "Thread-5980 (HornetQ-remoting-threads-HornetQServerImpl::serverUUID=a866a1bd-a922-11e7-8b97-0050569e60a9-2139288264-2029178161)",
                snapshot.getJavaThreads().get(0).getName());
    }
}
