/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.common.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;

import static org.eclipse.jifa.common.Constant.*;

public class DefaultAuthProviderImpl implements DefaultAuthProvider {
    @Override
    public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(new User() {
            @Override
            public JsonObject attributes() {
                return null;
            }

            @Override
            public User isAuthorized(Authorization authority, Handler<AsyncResult<Boolean>> resultHandler) {
                return null;
            }

            @Override
            public JsonObject principal() {
                return new JsonObject()
                    .put(USER_ID_KEY, "12345")
                    .put(USER_NAME_KEY, "admin")
                    .put(USER_IS_ADMIN_KEY, true);
            }

            @Override
            public void setAuthProvider(AuthProvider authProvider) {

            }
        }));
    }
}
