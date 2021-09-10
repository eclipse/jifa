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
package org.eclipse.jifa.master.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;
import org.eclipse.jifa.common.enums.StartStatus;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.WorkerServiceImpl;

import java.util.List;

@ProxyGen
@VertxGen
public interface WorkerService {

    @GenIgnore
    static void create(Vertx vertx, JDBCClient dbClient, Pivot pivot) {
        new ServiceBinder(vertx.getDelegate()).setAddress(WorkerService.class.getSimpleName())
                                              .register(WorkerService.class, new WorkerServiceImpl(dbClient, pivot));
    }

    @GenIgnore
    static void createProxy(Vertx vertx) {
        ProxyDictionary.add(WorkerService.class, new org.eclipse.jifa.master.service.reactivex.WorkerService(
            new WorkerServiceVertxEBProxy(vertx.getDelegate(), WorkerService.class.getSimpleName())));
    }

    void startWorker(String workerName, Handler<AsyncResult<Void>> handler);

    void startWorkerWithSpec(String workerName, long requestMemSize, Handler<AsyncResult<Void>> handler);

    void stopWorker(String workerName, Handler<AsyncResult<Void>> handler);

    void startWorkerDone(String workerName, Handler<AsyncResult<StartStatus>> handler);
}
