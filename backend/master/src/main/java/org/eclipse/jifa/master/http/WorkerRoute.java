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
package org.eclipse.jifa.master.http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.request.MakeHttpResponse;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.User;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.service.ServiceCenter;
import org.eclipse.jifa.master.service.WorkerService;

import java.util.List;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class WorkerRoute extends BaseRoute {

    private WorkerService workerService;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        workerService = ServiceCenter.lookup(WorkerService.class);
        apiRouter.get().path(Constant.QUERY_ALL_WORKERS).handler(ctx -> runHttpHandlerAsync(ctx, WorkerRoute.this::queryAll));
        apiRouter.post().path(Constant.WORKER_DISK_CLEANUP).handler(ctx -> runHttpHandlerAsync(ctx, WorkerRoute.this::diskCleanup));
    }

    @AsyncHttpHandler
    private void queryAll(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        List<Worker> workers = workerService.queryAll();
        MakeHttpResponse.ok(context, workers);
    }

    @AsyncHttpHandler
    private void diskCleanup(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        HttpServerRequest request = context.request();
        String hostIP = request.getParam("host_ip");
        workerService.diskCleanup(hostIP);
        MakeHttpResponse.ok(context, "ok");
    }
}
