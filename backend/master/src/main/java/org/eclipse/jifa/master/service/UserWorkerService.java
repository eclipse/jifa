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
import org.eclipse.jifa.master.entity.UserWorker;
import org.eclipse.jifa.master.service.impl.UserWorkerServiceImpl;

import java.util.List;

@ProxyGen
@VertxGen
public interface UserWorkerService {

    @GenIgnore
    static void create(Vertx vertx, JDBCClient dbClient) {
        new ServiceBinder(vertx.getDelegate()).setAddress(UserWorkerService.class.getSimpleName())
                                              .register(UserWorkerService.class, new UserWorkerServiceImpl(dbClient));
    }

    @GenIgnore
    static void createProxy(Vertx vertx) {
        ProxyDictionary.add(UserWorkerService.class, new org.eclipse.jifa.master.service.reactivex.UserWorkerService(
            new UserWorkerServiceVertxEBProxy(vertx.getDelegate(), UserWorkerService.class.getSimpleName())));
    }

    void add(String hostIP, int port, List<String> userIds, String operator, Handler<AsyncResult<Void>> handler);

    void delete(String hostIP, int port, Handler<AsyncResult<Void>> handler);

    void updateUserIds(String hostIP, int port, List<String> userIds, String operator,
                       Handler<AsyncResult<Void>> handler);

    void enable(String hostIP, int port, String operator, Handler<AsyncResult<Void>> handler);

    void disable(String hostIP, int port, String operator, Handler<AsyncResult<Void>> handler);

    void updateUserIdsAndState(String hostIP, int port, List<String> userIds, boolean enabled, String operator,
                               Handler<AsyncResult<Void>> handler);

    void query(String hostIP, int port, Handler<AsyncResult<UserWorker>> handler);

    void queryAll(Handler<AsyncResult<List<UserWorker>>> handler);
}
