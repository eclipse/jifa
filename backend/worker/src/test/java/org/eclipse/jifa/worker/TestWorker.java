/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

@RunWith(VertxUnitRunner.class)
public class TestWorker {

    private static Logger LOGGER = LoggerFactory.getLogger(TestWorker.class);

    @Before
    public void setup(TestContext context) throws Exception {
        FakeHooks.reset();
    }

    @Test
    public void testStartupBasicConfig(TestContext context) throws Exception {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("server.port", findRandomPort());
        cfg.put("server.host", "127.0.0.1");
        cfg.put("api.prefix", "/jifa-api");

        Async async = context.async();

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Worker.class.getName(),
                             new DeploymentOptions().setConfig(new JsonObject(cfg)).setInstances(1),
                             res -> {
                                 if (res.succeeded()) {
                                     vertx.undeploy(res.result(), res2 -> {
                                         if (res2.succeeded()) {
                                             async.complete();
                                         } else {
                                             context.fail(res2.cause());
                                         }
                                     });
                                 } else {
                                     context.fail(res.cause());
                                 }
                             });
    }

    @Test
    public void testStartWithHook(TestContext context) throws Exception {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("server.port", findRandomPort());
        cfg.put("server.host", "127.0.0.1");
        cfg.put("api.prefix", "/jifa-api");
        cfg.put("hooks.className", FakeHooks.class.getName());

        Async async = context.async();

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Worker.class.getName(),
                             new DeploymentOptions().setConfig(new JsonObject(cfg)).setInstances(1),
                             res -> {
                                 if (res.succeeded()) {
                                     vertx.undeploy(res.result(), res2 -> {
                                         if (res2.succeeded()) {
                                             context.assertEquals(FakeHooks.countInitTriggered(), 1);
                                             async.complete();
                                         } else {
                                             context.fail(res2.cause());
                                         }
                                     });
                                 } else {
                                     context.fail(res.cause());
                                 }
                             });
    }

    int findRandomPort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}
