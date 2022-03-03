/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.eclipse.jifa.common.JifaHooks;
import org.eclipse.jifa.worker.support.FileSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

public class WorkerGlobal {

    private static final Logger LOGGER = LoggerFactory.getLogger(Worker.class);

    public static Vertx VERTX;

    public static String HOST;

    public static int PORT;

    private static JsonObject CONFIG;

    private static String WORKSPACE;

    private static JifaHooks HOOKS;

    private static boolean initialized;

    static synchronized void reset() {
        if (!initialized) {
            return;
        }
        VERTX = null;
        HOST = null;
        PORT = 0;
        CONFIG = null;
        HOOKS = null;

        initialized = false;
    }

    static synchronized void init(Vertx vertx, String host, int port, JsonObject config, JifaHooks hooks) {
        if (initialized) {
            return;
        }

        VERTX = vertx;
        HOST = host;
        PORT = port;
        CONFIG = config;
        HOOKS = hooks;

        WORKSPACE = CONFIG.getString(Constant.ConfigKey.WORKSPACE, org.eclipse.jifa.common.Constant.DEFAULT_WORKSPACE);
        LOGGER.debug("Workspace: {}", WORKSPACE);

        File workspaceDir = new File(WORKSPACE);
        if (workspaceDir.exists()) {
            ASSERT.isTrue(workspaceDir.isDirectory(), "Workspace must be directory");
        } else {
            ASSERT.isTrue(workspaceDir.mkdirs(),
                          () -> "Can not create workspace: " + workspaceDir.getAbsolutePath());
        }

        FileSupport.init();

        initialized = true;
    }

    public static String stringConfig(String key) {
        return CONFIG.getString(key);
    }

    public static String stringConfig(String... keys) {
        JsonObject o = CONFIG;
        for (int i = 0; i < keys.length - 1; i++) {
            o = o.getJsonObject(keys[i]);
        }

        return o.getString(keys[keys.length - 1]);
    }

    public static int intConfig(String... keys) {
        JsonObject o = CONFIG;
        for (int i = 0; i < keys.length - 1; i++) {
            o = o.getJsonObject(keys[i]);
        }

        return o.getInteger(keys[keys.length - 1]);
    }

    public static boolean booleanConfig(String... keys) {
        JsonObject o = CONFIG;
        for (int i = 0; i < keys.length - 1; i++) {
            o = o.getJsonObject(keys[i]);
        }
        if (o == null) {
            return false;
        }

        return o.getBoolean(keys[keys.length - 1]);
    }

    public static String workspace() {
        return WORKSPACE;
    }

    public static JifaHooks hooks() {
        return HOOKS;
    }
}
