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
package org.eclipse.jifa.master;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.eclipse.jifa.master.http.HttpServerVerticle;
import org.eclipse.jifa.master.service.ServiceVerticle;
import org.eclipse.jifa.master.support.$;
import org.eclipse.jifa.master.support.WorkerClient;

public class Master extends AbstractVerticle implements Constant {

    private static final String DEV_CONF = "master-config-dev.json";

    public static boolean DEV_MODE = false;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Master.class.getName());
    }

    @Override
    public void start(Promise<Void> startFuture) {

        String mc = System.getProperty("jifa.master.config", DEV_CONF);
        DEV_MODE = DEV_CONF.endsWith(mc);

        ConfigRetriever configRetriever =
                ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(
                        new ConfigStoreOptions().setType("file")
                                .setConfig(new JsonObject().put("path", mc))));

        try {
            Future<JsonObject> future = $.asyncVoid(configRetriever::getConfig);
            JsonObject masterConfig = $.await(future);

            WorkerClient.init(masterConfig.getJsonObject(WORKER_CONFIG_KEY), WebClient.create(vertx));

            // service
            vertx.deployVerticle(ServiceVerticle.class.getName(), new DeploymentOptions()
                    .setConfig(masterConfig.getJsonObject(DB_KEYWORD)), asyncResult1 -> {
                if (asyncResult1.succeeded()) {
                    DeploymentOptions httpConfig =
                            new DeploymentOptions().setConfig(masterConfig.getJsonObject(HTTP_VERTICLE_CONFIG_KEY))
                                    .setInstances(Runtime.getRuntime().availableProcessors());
                    vertx.deployVerticle(HttpServerVerticle.class.getName(), httpConfig, asyncResult2 -> {
                        if (asyncResult2.succeeded()) {
                            startFuture.complete();
                        } else {
                            startFuture.fail(asyncResult2.cause());
                        }
                    });
                } else {
                    startFuture.fail(asyncResult1.cause());
                }
            });
        } catch (Throwable e) {
            startFuture.fail(e);
        }
    }
}
