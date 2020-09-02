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

import com.google.common.io.Files;
import com.sun.management.HotSpotDiagnosticMXBean;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.enums.ProgressState;
import org.eclipse.jifa.worker.Global;
import org.eclipse.jifa.worker.Starter;
import org.eclipse.jifa.worker.support.FileSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.lang.management.ManagementFactory;

@RunWith(VertxUnitRunner.class)
public class TestStarter {

    private static Logger LOGGER = LoggerFactory.getLogger(TestStarter.class);

    @Before
    public void setup(TestContext context) throws Exception {
        FakeHooks.reset();
    }

    @Test
    public void testStartupBasicConfig(TestContext context) throws Exception {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("server.port", 8888);
        cfg.put("api.prefix", "/jifa-api");
        
        Async async = context.async();

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Starter.class.getName(),
            new DeploymentOptions().setConfig(new JsonObject(cfg)).setInstances(1),
            res -> {
                context.assertTrue(res.succeeded());
                vertx.close(res2 -> {
                    async.complete();
                });
            });
    }

    @Test
    public void testStartWithHook(TestContext context) throws Exception {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("server.port", 8888);
        cfg.put("api.prefix", "/jifa-api");
        cfg.put("hooks.className", FakeHooks.class.getName());
        
        Async async = context.async();

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Starter.class.getName(),
            new DeploymentOptions().setConfig(new JsonObject(cfg)).setInstances(1),
            res -> {
                context.assertTrue(res.succeeded());
                context.assertEquals(FakeHooks.countInitTriggered(), 1);
                vertx.close(res2 -> {
                    async.complete();
                });
            });
    }

}
