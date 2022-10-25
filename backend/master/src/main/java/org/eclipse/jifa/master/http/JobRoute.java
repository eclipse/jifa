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

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.enums.JobState;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.service.ProxyDictionary;
import org.eclipse.jifa.master.service.reactivex.JobService;
import org.eclipse.jifa.master.vo.PendingJob;
import org.eclipse.jifa.master.vo.PendingJobsResult;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class JobRoute extends BaseRoute implements Constant {

    private JobService jobService;

    void init(Vertx vertx, JsonObject config, Router apiRouter) {
        jobService = ProxyDictionary.lookup(JobService.class);
        apiRouter.get().path(PENDING_JOBS).handler(this::frontPendingJobs);
    }

    private void frontPendingJobs(RoutingContext context) {

        JobType type = JobType.valueOf(context.request().getParam("type"));
        String target = context.request().getParam("target");

        jobService.rxFindActive(type, target)
                  .doOnSuccess(this::assertJobExist)
                  .flatMap(job -> {
                      if (job.getState() == JobState.IN_PROGRESS) {
                          return Single.just(new PendingJobsResult(true));
                      }
                      ASSERT.isTrue(job.getState() == JobState.PENDING, ErrorCode.SANITY_CHECK);
                      return jobService.rxPendingJobsInFrontOf(job)
                                       .map(fronts -> {
                                           ArrayList<Job> jobs = new ArrayList<>(fronts);
                                           jobs.add(job);
                                           return jobs;
                                       })
                                       .map(fronts -> fronts.stream().map(PendingJob::new).collect(Collectors.toList()))
                                       .map(PendingJobsResult::new);
                  })
                  .subscribe(result -> HTTPRespGuarder.ok(context, result),
                             t -> HTTPRespGuarder.fail(context, t));
    }
}
