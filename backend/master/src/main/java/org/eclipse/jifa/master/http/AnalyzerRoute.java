/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.File;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.enums.JobState;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.model.User;
import org.eclipse.jifa.master.service.ProxyDictionary;
import org.eclipse.jifa.master.service.reactivex.FileService;
import org.eclipse.jifa.master.service.reactivex.JobService;
import org.eclipse.jifa.master.support.Utils;
import org.eclipse.jifa.master.support.WorkerClient;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;
import static org.eclipse.jifa.master.entity.enums.JobType.*;

class AnalyzerRoute extends BaseRoute {

    private JobService jobService;

    private FileService fileService;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        jobService = ProxyDictionary.lookup(JobService.class);
        fileService = ProxyDictionary.lookup(FileService.class);
        // heap dump
        // Do not change the order !!
        apiRouter.route().path(HEAP_DUMP_RELEASE).handler(context -> release(context, HEAP_DUMP_ANALYSIS));
        apiRouter.route().path(HEAP_DUMP_COMMON).handler(context -> process(context, HEAP_DUMP_ANALYSIS));

        // gclog
        apiRouter.route().path(GCLOG_RELEASE).handler(context -> release(context, GCLOG_ANALYSIS));
        apiRouter.route().path(GCLOG_COMMON).handler(context -> process(context, GCLOG_ANALYSIS));

        // thread dump
        apiRouter.route().path(THREAD_DUMP_RELEASE).handler(context -> release(context, THREAD_DUMP_ANALYSIS));
        apiRouter.route().path(THREAD_DUMP_COMMON).handler(context -> process(context, THREAD_DUMP_ANALYSIS));
    }

    private Single<Job> findOrAllocate(User user, File file, JobType jobType) {
        String target = file.getName();
        return jobService.rxFindActive(jobType, target)
                         .flatMap(job -> job.found() ?
                                         Single.just(job) :
                                         jobService.rxAllocate(user.getId(), file.getHostIP(), jobType,
                                                               target, EMPTY_STRING, Utils.calculateLoadFromSize(file.getSize()), false)
                         );
    }

    private void release(RoutingContext context, JobType jobType) {
        User user = context.get(Constant.USER_INFO_KEY);
        String fileName = context.request().getParam("file");

        fileService.rxFile(fileName)
                   .doOnSuccess(file -> assertFileAvailable(file))
                   .doOnSuccess(file -> checkPermission(user, file))
                   .flatMap(file -> jobService.rxFindActive(jobType, file.getName()))
                   .doOnSuccess(this::assertJobExist)
                   .doOnSuccess(job -> ASSERT.isTrue(job.getState() != JobState.PENDING,
                                                     ErrorCode.RELEASE_PENDING_JOB))
                   .ignoreElement()
                   .andThen(jobService.rxFinish(jobType, fileName))
                   .subscribe(() -> HTTPRespGuarder.ok(context),
                              t -> HTTPRespGuarder.fail(context, t));

    }

    private void process(RoutingContext context, JobType jobType) {
        User user = context.get(Constant.USER_INFO_KEY);
        context.request().params().add("userName", user.getName());
        String fileName = context.request().getParam("file");

        fileService.rxFile(fileName)
                   .doOnSuccess(file -> assertFileAvailable(file))
                   .doOnSuccess(file -> checkPermission(user, file))
                   .doOnSuccess(file -> ASSERT.isTrue(file.transferred(), ErrorCode.NOT_TRANSFERRED))
                   .flatMap(file -> findOrAllocate(user, file, jobType))
                   .doOnSuccess(this::assertJobInProgress)
                   .flatMap(job -> WorkerClient.send(context.request(), job.getHostIP()))
                   .subscribe(resp -> HTTPRespGuarder.ok(context, resp.statusCode(), resp.bodyAsString()),
                              t -> HTTPRespGuarder.fail(context, t));
    }
}
