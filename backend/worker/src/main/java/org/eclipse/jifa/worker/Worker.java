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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.eclipse.jifa.common.JifaHooks;
import org.eclipse.jifa.common.aux.JifaException;
import org.eclipse.jifa.common.util.FileUtil;
import org.eclipse.jifa.worker.route.RouteFiller;
import org.eclipse.jifa.worker.support.hda.AnalysisEnv;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;

import static org.eclipse.jifa.worker.Constant.ConfigKey.*;
import static org.eclipse.jifa.worker.Constant.Misc.*;
import static org.eclipse.jifa.worker.WorkerGlobal.stringConfig;

public class Worker extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(Worker.class);
    private static final CountDownLatch count = new CountDownLatch(Runtime.getRuntime().availableProcessors());
    private static long startTime;

    public static void main(String[] args) throws InterruptedException {
        startTime = System.currentTimeMillis();

        if (!AnalysisEnv.INITIALIZED) {
            System.err.println("Heap dump analysis env initialized failed");
            return;
        }

        JsonObject vertxConfig = new JsonObject(
            FileUtil.content(Worker.class.getClassLoader().getResourceAsStream(DEFAULT_VERTX_CONFIG_FILE)));
        Vertx vertx = Vertx.vertx(new VertxOptions(vertxConfig));

        JsonObject jifaConfig = new JsonObject(
            FileUtil.content(Worker.class.getClassLoader().getResourceAsStream(DEFAULT_WORKER_CONFIG_FILE)));
        jifaConfig.getJsonObject(BASIC_AUTH).put(ENABLED, false);
        vertx.deployVerticle(Worker.class.getName(), new DeploymentOptions().setConfig(jifaConfig).setInstances(
            Runtime.getRuntime().availableProcessors()));

        count.await();
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

    private void setupBasicAuthHandler(Router router) {
        AuthHandler authHandler = BasicAuthHandler.create((authInfo, resultHandler) -> {
            Future<User> result = Future.future();
            if (stringConfig(BASIC_AUTH, USERNAME).equals(authInfo.getString(USERNAME)) &&
                stringConfig(BASIC_AUTH, PASSWORD).equals(authInfo.getString(PASSWORD))) {
                result.complete(new AbstractUser() {
                    @Override
                    public JsonObject principal() {
                        return null;
                    }

                    @Override
                    public void setAuthProvider(AuthProvider authProvider) {
                    }

                    @Override
                    protected void doIsPermitted(String permission, Handler<AsyncResult<Boolean>> resultHandler) {
                    }
                });
            } else {
                result.fail(new JifaException("Illegal User"));
            }
            resultHandler.handle(result);
        });
        router.route().handler(authHandler);
    }

    @Override
    public void start() {
        String host = config().containsKey(SERVER_HOST_KEY) ? config().getString(SERVER_HOST_KEY) : DEFAULT_HOST;

        int port = config().containsKey(SERVER_PORT_KEY) ? config().getInteger(SERVER_PORT_KEY) : randomPort();

        String staticRoot = System.getProperty(WEB_ROOT_KEY, "webroot");

        String uploadDir =
            config().containsKey(SERVER_UPLOAD_DIR_KEY) ? config().getString(SERVER_UPLOAD_DIR_KEY) : null;

        JifaHooks hooks = findHooks();

        vertx.executeBlocking(event -> {
            WorkerGlobal.init(vertx, host, port, config(), hooks);

            HttpServer server = vertx.createHttpServer(hooks.serverOptions());
            Router router = Router.router(vertx);

            // body handler always ends to be first so it can read the body
            if (uploadDir == null) {
                router.post().handler(BodyHandler.create());
            } else {
                router.post().handler(BodyHandler.create(uploadDir));
            }

            hooks.beforeRoutes(router);

            File webRoot = new File(staticRoot);
            if (webRoot.exists() && webRoot.isDirectory()) {
                StaticHandler staticHandler = StaticHandler.create();
                staticHandler.setAllowRootFileSystemAccess(true);
                staticHandler.setWebRoot(staticRoot);
                // non-api
                String staticPattern = "^(?!" + WorkerGlobal.stringConfig(Constant.ConfigKey.API_PREFIX) + ").*$";
                router.routeWithRegex(staticPattern)
                      .handler(staticHandler)
                      // route to "/" if not found
                      .handler(context -> context.reroute("/"));
            }

            // cors
            router.route().handler(CorsHandler.create("*"));

            // basic auth
            if (WorkerGlobal.booleanConfig(BASIC_AUTH, Constant.ConfigKey.ENABLED)) {
                setupBasicAuthHandler(router);
            }

            router.post().handler(BodyHandler.create());

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
                LOGGER.info("Jifa-Worker startup successfully in {} ms, verticle count = {}, http server = {}:{}",
                            System.currentTimeMillis() - startTime,
                            Runtime.getRuntime().availableProcessors(),
                            WorkerGlobal.HOST,
                            WorkerGlobal.PORT);
                count.countDown();
            } else {
                LOGGER.error("Failed to start Jifa' worker side", ar.cause());
                System.exit(-1);
            }
        });
    }

}
