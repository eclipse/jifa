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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

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
        Map<String, Object> auth = new HashMap<>();
        auth.put("enabled", false);
        cfg.put("basicAuth", auth);
        Map<String, Object> cache = new HashMap<>();
        cache.put("expireAfterAccess", 10);
        cache.put("expireAfterAccessTimeUnit", "MINUTES");
        cfg.put("cacheConfig", cache);

        Async async = context.async();

        Vertx vertx = Vertx.vertx();
        CountDownLatch count = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        Worker.setCount(count);
        vertx.deployVerticle(Worker.class.getName(),
                             new DeploymentOptions().setConfig(new JsonObject(cfg)).setInstances(1),
                             res -> {
                                 if (res.succeeded()) {
                                     try {
                                         count.await();
                                     } catch (InterruptedException e) {
                                         context.fail(e);
                                         return;
                                     }
                                     vertx.undeploy(res.result(), res2 -> {
                                         if (res2.succeeded()) {
                                             done.countDown();
                                             async.complete();
                                         } else {
                                             context.fail(res2.cause());
                                         }
                                     });
                                 } else {
                                     context.fail(res.cause());
                                 }
                             });
        done.await();
    }

    @Test
    public void testStartWithHook(TestContext context) throws Exception {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("server.port", findRandomPort());
        cfg.put("server.host", "127.0.0.1");
        cfg.put("api.prefix", "/jifa-api");
        Map<String, Object> auth = new HashMap<>();
        auth.put("enabled", false);
        cfg.put("basicAuth", auth);
        cfg.put("hooks.className", FakeHooks.class.getName());
        Map<String, Object> cache = new HashMap<>();
        cache.put("expireAfterAccess", 10);
        cache.put("expireAfterAccessTimeUnit", "MINUTES");
        cfg.put("cacheConfig", cache);

        Async async = context.async();

        Vertx vertx = Vertx.vertx();
        CountDownLatch count = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        Worker.setCount(count);
        vertx.deployVerticle(Worker.class.getName(),
                             new DeploymentOptions().setConfig(new JsonObject(cfg)).setInstances(1),
                             res -> {
                                 if (res.succeeded()) {
                                     try {
                                         count.await();
                                     } catch (InterruptedException e) {
                                         context.fail(e);
                                         return;
                                     }
                                     vertx.undeploy(res.result(), res2 -> {
                                         if (res2.succeeded()) {
                                             context.assertEquals(FakeHooks.countInitTriggered(), 1);
                                             done.countDown();
                                             async.complete();
                                         } else {
                                             context.fail(res2.cause());
                                         }
                                     });
                                 } else {
                                     context.fail(res.cause());
                                 }
                             });
        done.await();
    }

    @After
    public void reset() {
        Worker.resetCount();
    }

    int findRandomPort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}
