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

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.eclipse.jifa.common.request.MakeHttpResponse;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.User;
import org.eclipse.jifa.master.entity.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UserRoute implements Constant {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoute.class);

    private JWTAuth jwtAuth;

    private JWTOptions jwtOptions;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        // symmetric is not safe, but it's enough now...
        PubSecKeyOptions pubSecKeyOptions = new PubSecKeyOptions();
        pubSecKeyOptions.setAlgorithm(JWT_ALGORITHM_HS256)
                .setBuffer(JWT_ALGORITHM_HS256_PUBLIC_KEY);
        jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions().addPubSecKey(pubSecKeyOptions));
        jwtOptions = new JWTOptions();
        jwtOptions.setSubject(JWT_SUBJECT).setIssuer(JWT_ISSUER).setExpiresInMinutes(JWT_EXPIRES_IN_MINUTES);
        apiRouter.routeWithRegex("^(?!" + AUTH + "$).*").handler(JWTAuthHandler.create(jwtAuth));

        // These guys are really special, we do not need runHttpHandlerAsync here.
        apiRouter.post().path(AUTH).handler(this::auth);

        apiRouter.route().handler(this::extractUserInfo);

        apiRouter.get().path(USER_INFO).handler(this::userInfo);
    }

    private void auth(RoutingContext context) {
        var request = context.request();
        try {
            String username = request.getParam("username");
            String password = request.getParam("password");
            if ("admin".equals(username) && "admin".equals(password)) {
                JsonObject userInfo = new JsonObject()
                        .put(USER_ID_KEY, "12345")
                        .put(USER_NAME_KEY, "admin")
                        .put(Constant.USER_IS_ADMIN_KEY, true);
                UserToken token = new UserToken(jwtAuth.generateToken(userInfo, jwtOptions));
                MakeHttpResponse.ok(context, token);
            } else {
                MakeHttpResponse.ok(context);
            }
        } catch (Throwable e) {
            MakeHttpResponse.fail(context, e);
        }
    }

    private void extractUserInfo(RoutingContext context) {
        JsonObject principal = context.user().principal();
        User user = new User(principal.getString(USER_ID_KEY), principal.getString(USER_NAME_KEY),
                principal.getBoolean(USER_IS_ADMIN_KEY));
        context.put(USER_INFO_KEY, user);
        context.next();
    }

    private void userInfo(RoutingContext context) {
        MakeHttpResponse.ok(context, context.get(USER_INFO_KEY));
    }
}
