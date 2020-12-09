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
package org.eclipse.jifa.master.service.impl;

import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.enums.JobState;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.service.JobService;
import org.eclipse.jifa.master.service.impl.helper.JobHelper;
import org.eclipse.jifa.master.service.impl.helper.SQLHelper;
import org.eclipse.jifa.master.service.sql.JobSQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;

public class JobServiceImpl implements JobService, JobSQL, Constant {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobServiceImpl.class);

    private final JDBCClient dbClient;

    private final Pivot pivot;

    public JobServiceImpl(Pivot pivot, JDBCClient dbClient) {
        this.pivot = pivot;
        this.dbClient = dbClient;
    }

    @Override
    public void findActive(JobType jobType, String target, Handler<AsyncResult<Job>> handler) {
        dbClient.rxQueryWithParams(SELECT_ACTIVE_BY_TYPE_AND_TARGET, ja(jobType, target))
                .flatMap(resultSet -> {
                    if (resultSet.getNumRows() == 0) {
                        return Single.just(Job.NOT_FOUND);
                    }
                    Job job = JobHelper.fromDBRecord(resultSet.getRows().get(0));
                    if (job.getState() != JobState.IN_PROGRESS) {
                        return Single.just(job);
                    }
                    return dbClient.rxUpdateWithParams(UPDATE_ACCESS_TIME, ja(jobType, target))
                                   .ignoreElement()
                                   .toSingleDefault(job);
                })
                .subscribe(SingleHelper.toObserver(handler));
    }

    @Override
    public void pendingJobsInFrontOf(Job job, Handler<AsyncResult<List<Job>>> handler) {
        String hostIP = job.getHostIP();
        Instant instant = Instant.ofEpochMilli(job.getCreationTime());
        Single<ResultSet> single = hostIP == null ?
                                   dbClient.rxQueryWithParams(SELECT_FRONT_PENDING, ja(instant)) :
                                   dbClient.rxQueryWithParams(SELECT_FRONT_PENDING_BY_HOST_IP, ja(hostIP, instant));
        single.map(records -> records.getRows().stream().map(JobHelper::fromDBRecord)
                                     .filter(fontJob -> job.getId() != fontJob.getId())
                                     .collect(Collectors.toList()))
              .subscribe(SingleHelper.toObserver(handler));
    }

    @Override
    public void allocate(String userId, String hostIP, JobType jobType, String target, String attachment,
                         long estimatedLoad, boolean immediate, Handler<AsyncResult<Job>> handler) {
        pivot.allocate(userId, hostIP, jobType, target, attachment, estimatedLoad, immediate)
             .subscribe(SingleHelper.toObserver(handler));
    }

    @Override
    public void finish(JobType type, String target, Handler<AsyncResult<Void>> handler) {
        dbClient.rxQueryWithParams(SELECT_ACTIVE_BY_TYPE_AND_TARGET, ja(type, target))
                .map(SQLHelper::singleRow)
                .map(JobHelper::fromDBRecord)
                .flatMapCompletable(pivot::finish)
                .subscribe(CompletableHelper.toObserver(handler));
    }
}
