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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.Cookie;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;
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

        apiRouter.routeWithRegex(EXCLUDE_ROUTE_REGEX).handler(this::authWithCookie);
        apiRouter.routeWithRegex(EXCLUDE_ROUTE_REGEX).handler(JWTAuthHandler.create(jwtAuth));

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
        Single.just(context.request())
              .flatMap(req -> {
                  String username = req.getParam("username");
                  String password = req.getParam("password");
                  if ("admin".equals(username) && "admin".equals(password)) {
                      return Single.just(new JsonObject()
                                             .put(USER_ID_KEY, "12345")
                                             .put(USER_NAME_KEY, "admin")
                                             .put(Constant.USER_IS_ADMIN_KEY, true))
                                   .map(userInfo -> jwtAuth.generateToken(userInfo, jwtOptions));
                  } else {
                      return Single.just("");
                  }
              }).subscribe(token -> HTTPRespGuarder.ok(context, new UserToken(token)),
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
                cookie.setPath("/");
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
