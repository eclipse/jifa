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

package org.eclipse.jifa.common;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.eclipse.jifa.common.enums.FileType;

public interface JifaHooks {
    /* Access the server configuration at startup. This will be called by JIFA and configuration passed in.
       This will be called once for each verticle. */
    default void init(JsonObject config) {
    }

    /* Provide custom http server configuration to vertx. */
    default HttpServerOptions serverOptions(Vertx vertx) {
        return new HttpServerOptions();
    }

    /* Access the route configuration before JIFA routes are loaded.
       You could use this to customize redirects, authenticate, etc. */
    default void beforeRoutes(Vertx vertx, Router router) {
    }

    /* Access route configuration after JIFA routes are loaded.
       You could use this to customize error handling, etc. */
    default void afterRoutes(Vertx vertx, Router router) {
    }

    /* Provide custom mapping for directory path, file, and index functionality. */
    default String mapDirPath(FileType fileType, String name, String defaultPath) {
        return defaultPath;
    }

    default String mapFilePath(FileType fileType, String name, String childrenName, String defaultPath) {
        return defaultPath;
    }

    default String mapIndexPath(FileType fileType, String file, String defaultPath) {
        return defaultPath;
    }

    /* An empty default configuration */
    public class EmptyHooks implements JifaHooks {
        // use default implementations
    }
}
