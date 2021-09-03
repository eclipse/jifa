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
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.model.User;
import org.eclipse.jifa.master.service.ProxyDictionary;
import org.eclipse.jifa.master.service.reactivex.AdminService;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

public class AdminRoute extends BaseRoute {

    private AdminService adminService;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {

        adminService = ProxyDictionary.lookup(AdminService.class);

        apiRouter.post().path(Constant.ADD_ADMIN).handler(this::add);
        apiRouter.get().path(Constant.QUERY_ALL_ADMIN).handler(this::queryAll);
    }

    private void add(RoutingContext context) {
        User user = context.get(USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        String userId = context.request().getParam("userId");
        adminService.rxAdd(userId)
                    .subscribe(() -> HTTPRespGuarder.ok(context),
                               t -> HTTPRespGuarder.fail(context, t));
    }

    private void queryAll(RoutingContext context) {
        User user = context.get(USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        adminService.rxQueryAll()
                    .subscribe(admins -> HTTPRespGuarder.ok(context, admins),
                               t -> HTTPRespGuarder.fail(context, t));
    }
}
