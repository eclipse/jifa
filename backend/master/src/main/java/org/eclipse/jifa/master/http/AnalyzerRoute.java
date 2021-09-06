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
package org.eclipse.jifa.master.http;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.request.MakeHttpResponse;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.FileRecord;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.User;
import org.eclipse.jifa.master.entity.enums.JobState;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.service.FileService;
import org.eclipse.jifa.master.service.JobService;
import org.eclipse.jifa.master.service.ServiceCenter;
import org.eclipse.jifa.master.support.$;
import org.eclipse.jifa.master.support.WorkerClient;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;
import static org.eclipse.jifa.master.entity.enums.JobType.HEAP_DUMP_ANALYSIS;

class AnalyzerRoute extends BaseRoute {

    private JobService jobService;

    private FileService fileService;

    // TODO: current algorithm used isn't good enough
    private static long calculateLoad(FileRecord file) {
        double size = file.getSize();
        double G = 1024 * 1024 * 1024;

        long load = (long) Math.ceil(size / G) * 10;
        load = Math.max(load, 10);
        load = Math.min(load, 200);

        return load;
    }

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        jobService = ServiceCenter.lookup(JobService.class);
        fileService = ServiceCenter.lookup(FileService.class);
        // heap dump
        // Do not change the order !!
        apiRouter.route().path(HEAP_DUMP_RELEASE).handler(ctx -> runHttpHandlerAsync(ctx, AnalyzerRoute.this::release));
        apiRouter.route().path(HEAP_DUMP_COMMON).handler(ctx -> runHttpHandlerAsync(ctx, AnalyzerRoute.this::process));
    }

    @AsyncHttpHandler
    private Job findOrAllocate(User user, FileRecord file, JobType jobType) {
        String target = file.getName();
        Job job = jobService.findActive(jobType, target);
        if (job.found()) {
            return job;
        } else {
            job = jobService.allocate(user.getId(), file.getHostIP(), jobType,
                    target, EMPTY_STRING, calculateLoad(file), false);
            return job;
        }
    }

    @AsyncHttpHandler
    private void release(RoutingContext context) {
        final JobType jobType = HEAP_DUMP_ANALYSIS;
        User user = context.get(Constant.USER_INFO_KEY);
        String fileName = context.request().getParam("file");

        FileRecord file = fileService.file(fileName);
        assertFileAvailable(file);
        checkPermission(user, file);
        Job job = jobService.findActive(jobType, file.getName());
        assertJobExist(job);
        ASSERT.isTrue(job.getState() != JobState.PENDING,
                ErrorCode.RELEASE_PENDING_JOB);
        jobService.finish(jobType, fileName);
        MakeHttpResponse.ok(context);
    }

    @AsyncHttpHandler
    private void process(RoutingContext context) {
        User user = context.get(Constant.USER_INFO_KEY);
        context.request().params().add("userName", user.getName());
        String fileName = context.request().getParam("file");

        FileRecord file = fileService.file(fileName);
        assertFileAvailable(file);
        checkPermission(user, file);
        ASSERT.isTrue(file.transferred(), ErrorCode.NOT_TRANSFERRED);
        Job job = findOrAllocate(user, file, HEAP_DUMP_ANALYSIS);
        assertJobInProgress(job);
        Future<HttpResponse<Buffer>> future = $.asyncVoid(WorkerClient::send, context.request(), job.getHostIP());
        HttpResponse<Buffer> resp = $.await(future);
        MakeHttpResponse.ok(context, resp);
    }
}
