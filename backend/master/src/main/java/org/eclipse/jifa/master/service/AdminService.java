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
package org.eclipse.jifa.master.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;
import org.eclipse.jifa.master.entity.Admin;
import org.eclipse.jifa.master.service.impl.AdminServiceImpl;

import java.util.List;

@ProxyGen
@VertxGen
public interface AdminService {

    @GenIgnore
    static void create(Vertx vertx, JDBCClient dbClient) {
        new ServiceBinder(vertx.getDelegate()).setAddress(AdminService.class.getSimpleName())
                                              .register(AdminService.class, new AdminServiceImpl(dbClient));
    }

    @GenIgnore
    static void createProxy(Vertx vertx) {
        ProxyDictionary.add(AdminService.class, new org.eclipse.jifa.master.service.reactivex.AdminService(
            new AdminServiceVertxEBProxy(vertx.getDelegate(), AdminService.class.getSimpleName())));
    }

    void isAdmin(String userId, Handler<AsyncResult<Boolean>> handler);

    void add(String userId, Handler<AsyncResult<Void>> handler);

    void queryAll(Handler<AsyncResult<List<Admin>>> handler);
}
