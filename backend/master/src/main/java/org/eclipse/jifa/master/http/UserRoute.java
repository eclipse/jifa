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

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.Cookie;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.eclipse.jifa.common.auth.reactivex.DefaultAuthHandler;
import org.eclipse.jifa.common.auth.reactivex.DefaultAuthProvider;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.model.User;
import org.eclipse.jifa.master.vo.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UserRoute implements Constant {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoute.class);

    private static final String EXCLUDE_URI = String.join("|", HEALTH_CHECK, AUTH);

    private static final String EXCLUDE_ROUTE_REGEX = "^(?!" + EXCLUDE_URI +"$).*";

    private DefaultAuthProvider authProvider;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        authProvider = DefaultAuthProvider.create();

        apiRouter.routeWithRegex(EXCLUDE_ROUTE_REGEX).handler(this::authWithCookie);
        apiRouter.routeWithRegex(EXCLUDE_ROUTE_REGEX).handler(DefaultAuthHandler.create(authProvider));

        apiRouter.post().path(AUTH).handler(this::auth);

        apiRouter.routeWithRegex(EXCLUDE_ROUTE_REGEX).handler(this::extractInfo);

        apiRouter.get().path(USER_INFO).handler(this::userInfo);
    }

    private void authWithCookie(RoutingContext context) {
        Cookie authCookie = context.request().getCookie(COOKIE_AUTHORIZATION);
        if (!context.request().headers().contains(HEADER_AUTHORIZATION) && authCookie != null) {
            context.request().headers().add(HEADER_AUTHORIZATION, HEADER_AUTHORIZATION_PREFIX + authCookie.getValue());
        }
        context.next();
    }

    private void auth(RoutingContext context) {
        authProvider.rxAuthenticate(new TokenCredentials("OK"))
            .subscribe(token -> HTTPRespGuarder.ok(context, new UserToken(token.toString())),
            t -> HTTPRespGuarder.fail(context, t));
    }

    private void extractUserInfo(RoutingContext context) {
        JsonObject principal = context.user().principal();
        User user = new User(principal.getString(USER_ID_KEY), principal.getString(USER_NAME_KEY),
            principal.getBoolean(USER_IS_ADMIN_KEY));
        context.put(USER_INFO_KEY, user);
    }

    private void saveAuthorizationCookie(RoutingContext context) {
        Cookie authCookie = context.getCookie(COOKIE_AUTHORIZATION);
        String authHeader = context.request().getHeader(HEADER_AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(HEADER_AUTHORIZATION_PREFIX)) {
            // cookie can not have ' ', so we save substring here
            authHeader = authHeader.substring(HEADER_AUTHORIZATION_PREFIX.length());
            if (authCookie == null || !authHeader.equals(authCookie.getValue())) {
                Cookie cookie = Cookie.cookie(COOKIE_AUTHORIZATION, authHeader);
                context.addCookie(cookie);
            }
        }
    }

    private void extractInfo(RoutingContext context) {
        saveAuthorizationCookie(context);
        extractUserInfo(context);
        context.next();
    }

    private void userInfo(RoutingContext context) {
        HTTPRespGuarder.ok(context, context.get(USER_INFO_KEY));
    }
}
