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

import io.reactivex.Single;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.master.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerVerticle extends AbstractVerticle implements Constant {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

    private WebClient client;

    @Override
    public void start(Promise<Void> startFuture) {
        vertx.rxExecuteBlocking(future -> {
            client = WebClient.create(vertx);
            // base
            Router router = Router.router(vertx);

            router.errorHandler(Constant.HTTP_INTERNAL_SERVER_ERROR_STATUS_CODE, this::error);
            router.errorHandler(Constant.HTTP_BAD_REQUEST_STATUS_CODE, this::error);

            // jifa api
            Router apiRouter = Router.router(vertx);
            router.mountSubRouter(BASE, apiRouter);
            apiRouter.post().handler(BodyHandler.create());

            new UserRoute().init(vertx, config(), apiRouter);

            new JobRoute().init(vertx, config(), apiRouter);

            new AdminRoute().init(vertx, config(), apiRouter);

            new WorkerRoute().init(vertx, config(), apiRouter);

            new FileRoute().init(vertx, config(), apiRouter);

            new AnalyzerRoute().init(vertx, config(), apiRouter);

            Integer port = config().getInteger("port");
            vertx.createHttpServer().requestHandler(router).rxListen(port).subscribe(s -> {
                LOGGER.info("Master-Http-Server-Verticle started successfully, port is {}", port);
                future.complete(Single.just(this));
            }, future::fail);
        }).subscribe(f -> startFuture.complete(), startFuture::fail);
    }

    void error(RoutingContext context) {
        Throwable failure = context.failure();
        HttpServerResponse response = context.response();
        if (failure != null && !response.ended() && !response.closed()) {
            HTTPRespGuarder.fail(context, failure);
        }
    }
}
