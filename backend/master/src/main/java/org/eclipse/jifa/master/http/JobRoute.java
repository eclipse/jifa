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

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.request.MakeHttpResponse;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.PendingJob;
import org.eclipse.jifa.master.entity.PendingJobsResult;
import org.eclipse.jifa.master.entity.enums.JobState;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.service.JobService;
import org.eclipse.jifa.master.service.ServiceCenter;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class JobRoute extends BaseRoute implements Constant {

    private JobService jobService;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        jobService = ServiceCenter.lookup(JobService.class);
        apiRouter.get().path(PENDING_JOBS).handler(ctx -> runHttpHandlerAsync(ctx, JobRoute.this::frontPendingJobs));
    }

    @AsyncHttpHandler
    private void frontPendingJobs(RoutingContext context) {
        JobType type = JobType.valueOf(context.request().getParam("type"));
        String target = context.request().getParam("target");


        Job job = jobService.findActive(type, target);
        assertJobExist(job);
        if (job.getState() == JobState.IN_PROGRESS) {
            MakeHttpResponse.ok(context, new PendingJobsResult(true));
            return;
        }
        ASSERT.isTrue(job.getState() == JobState.PENDING, ErrorCode.SANITY_CHECK);
        List<Job> jobs = jobService.pendingJobsInFrontOf(job);
        jobs.add(job);
        List<PendingJob> pj = jobs.stream().map(PendingJob::new).collect(Collectors.toList());
        MakeHttpResponse.ok(context, new PendingJobsResult(pj));
    }
}
