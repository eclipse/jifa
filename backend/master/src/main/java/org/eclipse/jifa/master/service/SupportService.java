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
package org.eclipse.jifa.master.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.SupportServiceImpl;

@ProxyGen
@VertxGen
public interface SupportService {

    @GenIgnore
    static void create(Vertx vertx, JDBCClient dbClient, Pivot pivot) {
        new ServiceBinder(vertx.getDelegate())
            .setIncludeDebugInfo(true)
            .setAddress(SupportService.class.getSimpleName())
            .register(SupportService.class, new SupportServiceImpl(dbClient, pivot));
    }

    @GenIgnore
    static void createProxy(Vertx vertx) {
        ProxyDictionary.add(SupportService.class, new org.eclipse.jifa.master.service.reactivex.SupportService(
                new SupportServiceVertxEBProxy(vertx.getDelegate(), SupportService.class.getSimpleName())));
    }

    void isDBConnectivity(Handler<AsyncResult<Boolean>> handler);

    void startDummyWorker(Handler<AsyncResult<Void>> handler);

    void stopDummyWorker(Handler<AsyncResult<Void>> handler);
}
