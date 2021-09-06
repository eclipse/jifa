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
package org.eclipse.jifa.master.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import org.eclipse.jifa.common.request.MakeHttpResponse;
import org.eclipse.jifa.master.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerVerticle extends AbstractVerticle implements Constant {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

    private WebClient client;

    static void error(RoutingContext context) {
        Throwable failure = context.failure();
        HttpServerResponse response = context.response();
        if (failure != null && !response.ended() && !response.closed()) {
            MakeHttpResponse.fail(context, failure);
        }
    }

    @Override
    public void start(Promise<Void> startFuture) {
        vertx.executeBlocking(event -> {
            // Web client
            client = WebClient.create(vertx);

            // Http handlers.
            // All handlers run in a thread to avoid blocking the EventLoop thread.
            Router router = Router.router(vertx);
            router.errorHandler(Constant.HTTP_INTERNAL_SERVER_ERROR_STATUS_CODE, HttpServerVerticle::error);
            router.errorHandler(Constant.HTTP_BAD_REQUEST_STATUS_CODE, HttpServerVerticle::error);

            Router apiRouter = Router.router(vertx);
            router.mountSubRouter(BASE, apiRouter);
            router.route().failureHandler(e -> MakeHttpResponse.fail(e, e.failure()));

            apiRouter.post().handler(BodyHandler.create());

            new UserRoute().init(vertx, config(), apiRouter);
            new JobRoute().init(vertx, config(), apiRouter);
            new AdminRoute().init(vertx, config(), apiRouter);
            new WorkerRoute().init(vertx, config(), apiRouter);
            new FileRoute().init(vertx, config(), apiRouter);
            new AnalyzerRoute().init(vertx, config(), apiRouter);

            // Create Http server
            Integer port = config().getInteger("port");

            vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(port);

            LOGGER.info("Master http server started successfully, port is {}", port);

            event.complete();
        });
    }
}
