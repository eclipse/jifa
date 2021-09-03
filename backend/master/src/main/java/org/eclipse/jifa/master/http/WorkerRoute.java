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

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.model.User;
import org.eclipse.jifa.master.service.ProxyDictionary;
import org.eclipse.jifa.master.service.reactivex.WorkerService;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class WorkerRoute extends BaseRoute {

    private WorkerService workerService;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        workerService = ProxyDictionary.lookup(WorkerService.class);
        apiRouter.get().path(Constant.QUERY_ALL_WORKERS).handler(this::queryAll);
        apiRouter.post().path(Constant.WORKER_DISK_CLEANUP).handler(this::diskCleanup);
    }

    private void queryAll(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        workerService.rxQueryAll()
                     .subscribe(
                         workers -> HTTPRespGuarder.ok(context, workers),
                         t -> HTTPRespGuarder.fail(context, t)
                     );
    }

    private void diskCleanup(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        HttpServerRequest request = context.request();
        String hostIP = request.getParam("host_ip");
        workerService.rxDiskCleanup(hostIP).subscribe(
            () -> HTTPRespGuarder.ok(context, "ok"),
            t -> HTTPRespGuarder.fail(context, t));
    }

}
