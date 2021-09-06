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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.request.MakeHttpResponse;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.Admin;
import org.eclipse.jifa.master.entity.User;
import org.eclipse.jifa.master.service.AdminService;
import org.eclipse.jifa.master.service.ServiceCenter;

import java.util.List;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

public class AdminRoute extends BaseRoute {
    private Vertx vertx;

    private AdminService adminService;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        this.vertx = vertx;
        adminService = ServiceCenter.lookup(AdminService.class);

        apiRouter.post().path(Constant.ADD_ADMIN).handler(ctx -> runHttpHandlerAsync(ctx, AdminRoute.this::add));
        apiRouter.get().path(Constant.QUERY_ALL_ADMIN).handler(ctx -> runHttpHandlerAsync(ctx, AdminRoute.this::queryAll));
    }

    @AsyncHttpHandler
    private void add(RoutingContext context) {
        User user = context.get(USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        String userId = context.request().getParam("userId");
        adminService.add(userId);
        MakeHttpResponse.ok(context);
    }

    @AsyncHttpHandler
    private void queryAll(RoutingContext context) {
        User user = context.get(USER_INFO_KEY);
        ASSERT.isTrue(user.isAdmin(), ErrorCode.FORBIDDEN);
        List<Admin> admins = adminService.queryAll();
        MakeHttpResponse.ok(context, admins);
    }
}
