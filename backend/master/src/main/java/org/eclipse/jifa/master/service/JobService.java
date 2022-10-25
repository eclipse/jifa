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
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.service.impl.JobServiceImpl;
import org.eclipse.jifa.master.service.impl.Pivot;

import java.util.List;

@ProxyGen
@VertxGen
public interface JobService {
    @GenIgnore
    static void create(Vertx vertx, Pivot pivot, JDBCClient dbClient) {
        new ServiceBinder(vertx.getDelegate())
            .setAddress(JobService.class.getSimpleName())
            .register(JobService.class, new JobServiceImpl(pivot, dbClient));
    }

    @GenIgnore
    static void createProxy(Vertx vertx) {
        ProxyDictionary.add(JobService.class, new org.eclipse.jifa.master.service.reactivex.JobService(
            new JobServiceVertxEBProxy(vertx.getDelegate(), JobService.class.getSimpleName())));
    }

    void findActive(JobType jobType, String target, Handler<AsyncResult<Job>> handler);

    void pendingJobsInFrontOf(Job job, Handler<AsyncResult<List<Job>>> handler);

    void allocate(String userId, String hostIP, JobType jobType, String target,
                  String attachment, long estimatedLoad,
                  boolean immediate,
                  Handler<AsyncResult<Job>> handler);

    void finish(JobType type, String target, Handler<AsyncResult<Void>> handler);
}
