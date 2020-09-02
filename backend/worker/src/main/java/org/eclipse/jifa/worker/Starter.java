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

import com.google.common.base.Strings;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jifa.common.JifaHooks;
import org.eclipse.jifa.worker.route.RouteFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.ReflectiveOperationException;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import static org.eclipse.jifa.worker.Constant.ConfigKey.SERVER_HOST_KEY;
import static org.eclipse.jifa.worker.Constant.ConfigKey.SERVER_PORT_KEY;
import static org.eclipse.jifa.worker.Constant.ConfigKey.SERVER_UPLOAD_DIR_KEY;
import static org.eclipse.jifa.worker.Constant.ConfigKey.HOOKS_NAME_KEY;
import static org.eclipse.jifa.worker.Constant.Misc.*;

public class Starter extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(Starter.class);

    private static CountDownLatch count;

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        JsonObject vc = new JsonObject(readConfig(VERTX_CONFIG_KEY, DEFAULT_VERTX_CONFIG));
        Vertx vertx = Vertx.vertx(new VertxOptions(vc));
        JsonObject wc = new JsonObject(readConfig(WORKER_CONFIG_KEY, DEFAULT_WORKER_CONFIG));
        int processors = Runtime.getRuntime().availableProcessors();

        count = new CountDownLatch(processors);
        vertx.deployVerticle(Starter.class.getName(), new DeploymentOptions().setConfig(wc).setInstances(processors));
        count.await();

        LOGGER.info("Jifa-Worker startup successfully in {} ms, verticle count = {}, http server = {}:{}",
                    System.currentTimeMillis() - start, processors, Global.HOST, Global.PORT);
    }

    private static String readConfig(String key, String def) throws IOException {
        String v = System.getProperty(key);
        return Strings.isNullOrEmpty(v)
               ? IOUtils.toString(Objects.requireNonNull(Starter.class.getClassLoader().getResource(def)),
                                  Charset.defaultCharset())
               : FileUtils.readFileToString(new File(v), Charset.defaultCharset());
    }

    private static int randomPort() {
        try {
            ServerSocket socket = new ServerSocket(0);
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    JifaHooks findHooks() {
        JifaHooks hook = null;

        if (config().containsKey(HOOKS_NAME_KEY)) {
            String className = config().getString(HOOKS_NAME_KEY);
            try {
                LOGGER.info("applying hooks: " + className);
                Class<JifaHooks> clazz = (Class<JifaHooks>) Class.forName(className);
                hook = clazz.getConstructor().newInstance();
                hook.init(config());
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("could not start hook class: " + className + ", due to error", e);
            }
        }

        return hook != null ? hook : new JifaHooks.EmptyHooks();
    }

    @Override
    public void start() {
        String host = config().containsKey(SERVER_HOST_KEY) ? config().getString(SERVER_HOST_KEY) : DEFAULT_HOST;

        int port = config().containsKey(SERVER_PORT_KEY) ? config().getInteger(SERVER_PORT_KEY) : randomPort();

        String staticRoot = System.getProperty(WEB_ROOT_KEY, "webroot");

        String uploadDir = config().containsKey(SERVER_UPLOAD_DIR_KEY) ? config().getString(SERVER_UPLOAD_DIR_KEY) : null;

        JifaHooks hooks = findHooks();

        vertx.executeBlocking(event -> {
            Global.init(vertx, host, port, config(), hooks);

            HttpServer server = vertx.createHttpServer(hooks.serverOptions());
            Router router = Router.router(vertx);

            // body handler always eneds to be first so it can read the body
            if (uploadDir == null) {
                router.post().handler(BodyHandler.create());
            } else {
                router.post().handler(BodyHandler.create(uploadDir));
            }

            hooks.beforeRoutes(router);

            router.post().handler(BodyHandler.create());

            File webRoot = new File(staticRoot);
            if (webRoot.exists() && webRoot.isDirectory()) {
                String staticPattern = "^(?!" + Global.stringConfig(Constant.ConfigKey.API_PREFIX) + ").*$";
                StaticHandler staticHandler = StaticHandler.create();
                staticHandler.setAllowRootFileSystemAccess(true);
                staticHandler.setWebRoot(staticRoot);
                router.routeWithRegex(staticPattern).handler(staticHandler);
            }
            // cors
            router.route().handler(CorsHandler.create("*"));

            new RouteFiller(router).fill();
            hooks.afterRoutes(router);
            server.requestHandler(router);

            server.listen(port, host, ar -> {
                if (ar.succeeded()) {
                    event.complete();
                } else {
                    event.fail(ar.cause());
                }
            });
        }, ar -> {
            if (ar.succeeded()) {
                count.countDown();
            } else {
                LOGGER.error("Worker-Verticle startup failed", ar.cause());
                System.exit(-1);
            }
        });
    }

}
