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
import org.apache.commons.lang.StringUtils;
import org.eclipse.jifa.common.aux.ErrorCode;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.model.User;
import org.eclipse.jifa.master.service.ProxyDictionary;
import org.eclipse.jifa.master.service.reactivex.UserWorkerService;
import org.eclipse.jifa.master.support.WorkerClient;

import java.util.Arrays;
import java.util.List;

import static org.eclipse.jifa.common.aux.ErrorCode.*;
import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class UserWorkerRoute extends BaseRoute {

    private UserWorkerService userWorkerService;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {

        userWorkerService = ProxyDictionary.lookup(UserWorkerService.class);

        apiRouter.post().path(Constant.ADD_USER_WORKER).handler(this::add);
        apiRouter.post().path(Constant.DELETE_USER_WORKER).handler(this::delete);
        apiRouter.post().path(Constant.UPDATE_USER_WORKER_USER_IDS).handler(this::updateUserIds);
        apiRouter.post().path(Constant.ENABLE_USER_WORKER).handler(this::enable);
        apiRouter.post().path(Constant.DISABLE_USER_WORKER).handler(this::disable);
        apiRouter.post().path(Constant.UPDATE_USER_WORKER_USER_IDS_AND_STATE).handler(this::updateUserIdsAndState);
        apiRouter.get().path(Constant.QUERY_ALL_USER_WORKERS).handler(this::queryAll);
        apiRouter.get().path(Constant.CHECK_USER_WORKER).handler(this::check);

        apiRouter.route().path(Constant.FILES).handler(this::tryForward);
        apiRouter.route().path(Constant.FILE).handler(this::tryForward);
        apiRouter.route().path(Constant.PUBLIC_KEY).handler(this::tryForward);
        apiRouter.route().path(Constant.FILE_DELETE).handler(this::tryForward);
        apiRouter.route().path(Constant.TRANSFER_BY_URL).handler(this::tryForward);
        apiRouter.route().path(Constant.TRANSFER_BY_SCP).handler(this::tryForward);
        apiRouter.route().path(Constant.TRANSFER_BY_OSS).handler(this::tryForward);
        apiRouter.route().path(Constant.TRANSFER_PROGRESS).handler(this::tryForward);
        apiRouter.route().path(Constant.HEAP_DUMP_COMMON).handler(this::tryForward);
    }

    private void tryForward(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        HttpServerRequest request = context.request();
        String hostIP = request.getParam("specifiedWorkerIP");
        if (hostIP != null) {
            int port = Integer.valueOf(request.getParam("specifiedWorkerPort"));

            userWorkerService.rxQuery(hostIP, port)
                             .doOnSuccess(userWorker -> ASSERT.isTrue(userWorker.found(), WORKER_DOES_NOT_EXIST))
                             .doOnSuccess(userWorker -> checkPermission(user, userWorker))
                             .doOnSuccess(userWorker -> ASSERT.isTrue(userWorker.isEnabled(), WORKER_DISABLED))
                             .flatMap(userWorker -> WorkerClient.send(request, hostIP, port))
                             .subscribe(
                                 resp -> HTTPRespGuarder.ok(context, resp.bodyAsString()),
                                 t -> HTTPRespGuarder.fail(context, t)
                             );
        } else {
            context.next();
        }
    }

    private void add(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        HttpServerRequest request = context.request();
        String hostIP = request.getParam("hostIP");
        int port = Integer.valueOf(request.getParam("port"));
        List<String> userIds = Arrays.asList(StringUtils.split(request.getParam("userIds"), ","));

        userWorkerService.rxAdd(hostIP, port, userIds, user.getId())
                         .subscribe(
                             () -> HTTPRespGuarder.ok(context),
                             t -> HTTPRespGuarder.fail(context, t)
                         );
    }

    private void delete(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        HttpServerRequest request = context.request();
        String hostIP = request.getParam("hostIP");
        int port = Integer.valueOf(request.getParam("port"));

        userWorkerService.rxDelete(hostIP, port)
                         .subscribe(
                             () -> HTTPRespGuarder.ok(context),
                             t -> HTTPRespGuarder.fail(context, t)
                         );
    }

    private void updateUserIds(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        HttpServerRequest request = context.request();
        String hostIP = request.getParam("hostIP");
        int port = Integer.valueOf(request.getParam("port"));
        List<String> userIds = Arrays.asList(StringUtils.split(request.getParam("userIds"), ","));
        ASSERT.isTrue(!userIds.isEmpty(), ErrorCode.ILLEGAL_ARGUMENT, "User Ids can not be empty");

        userWorkerService.rxUpdateUserIds(hostIP, port, userIds, user.getId())
                         .subscribe(
                             () -> HTTPRespGuarder.ok(context),
                             t -> HTTPRespGuarder.fail(context, t)
                         );
    }

    private void enable(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        HttpServerRequest request = context.request();
        String hostIP = request.getParam("hostIP");
        int port = Integer.valueOf(request.getParam("port"));

        userWorkerService.rxEnable(hostIP, port, user.getId())
                         .subscribe(
                             () -> HTTPRespGuarder.ok(context),
                             t -> HTTPRespGuarder.fail(context, t)
                         );
    }

    private void disable(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        HttpServerRequest request = context.request();
        String hostIP = request.getParam("hostIP");
        int port = Integer.valueOf(request.getParam("port"));

        userWorkerService.rxDisable(hostIP, port, user.getId())
                         .subscribe(
                             () -> HTTPRespGuarder.ok(context),
                             t -> HTTPRespGuarder.fail(context, t)
                         );
    }

    private void updateUserIdsAndState(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        HttpServerRequest request = context.request();
        String hostIP = request.getParam("hostIP");
        int port = Integer.valueOf(request.getParam("port"));
        List<String> userIds = Arrays.asList(StringUtils.split(request.getParam("userIds"), ","));
        ASSERT.isTrue(!userIds.isEmpty(), ErrorCode.ILLEGAL_ARGUMENT, "User Ids can not be empty");
        boolean enabled = Boolean.valueOf(request.getParam("enabled"));

        userWorkerService.rxUpdateUserIdsAndState(hostIP, port, userIds, enabled, user.getId())
                         .subscribe(
                             () -> HTTPRespGuarder.ok(context),
                             t -> HTTPRespGuarder.fail(context, t)
                         );
    }

    private void queryAll(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        userWorkerService.rxQueryAll()
                         .subscribe(
                             userWorkers -> HTTPRespGuarder.ok(context, userWorkers),
                             t -> HTTPRespGuarder.fail(context, t)
                         );
    }

    private void check(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        HttpServerRequest request = context.request();
        String hostIP = request.getParam("specifiedWorkerIP");
        ASSERT.isTrue(hostIP != null, ILLEGAL_ARGUMENT);
        int port = Integer.valueOf(request.getParam("specifiedWorkerPort"));

        userWorkerService.rxQuery(hostIP, port)
                         .doOnSuccess(userWorker -> ASSERT.isTrue(userWorker.found(), WORKER_DOES_NOT_EXIST))
                         .doOnSuccess(userWorker -> checkPermission(user, userWorker))
                         .doOnSuccess(userWorker -> ASSERT.isTrue(userWorker.isEnabled(), WORKER_DISABLED))
                         .ignoreElement()
                         .subscribe(
                             () -> HTTPRespGuarder.ok(context),
                             t -> HTTPRespGuarder.fail(context, t)
                         );
    }
}
