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

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.enums.JobState;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.service.orm.JobHelper;
import org.eclipse.jifa.master.service.orm.SQLHelper;
import org.eclipse.jifa.master.support.$;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.orm.SQLHelper.makeSqlArgument;
import static org.eclipse.jifa.master.service.sql.JobSQL.*;

public final class JobService extends ServiceCenter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);


    public Job findActive(JobType jobType, String target) {
        Future<ResultSet> future = $.async(dbClient::queryWithParams, SELECT_ACTIVE_BY_TYPE_AND_TARGET, makeSqlArgument(jobType, target));
        ResultSet rs = $.await(future);
        if (rs.getNumRows() == 0) {
            return Job.NOT_FOUND;
        }

        Job job = JobHelper.fromDBRecord(rs.getRows().get(0));
        if (job.getState() != JobState.IN_PROGRESS) {
            return job;
        }
        Future<UpdateResult> future1 = $.async(dbClient::updateWithParams, UPDATE_ACCESS_TIME, makeSqlArgument(jobType, target));
        $.await(future1);
        return job;
    }

    public List<Job> pendingJobsInFrontOf(Job job) {
        String hostIP = job.getHostIP();
        Instant instant = Instant.ofEpochMilli(job.getCreationTime());
        String sql;
        JsonArray sqlParam;
        if (hostIP == null) {
            sql = SELECT_FRONT_PENDING;
            sqlParam = SQLHelper.makeSqlArgument(instant);
        } else {
            sql = SELECT_FRONT_PENDING_BY_HOST_IP;
            sqlParam = makeSqlArgument(hostIP, instant);
        }

        Future<ResultSet> future = $.async(dbClient::queryWithParams, sql, sqlParam);
        ResultSet rs = $.await(future);
        return rs
                .getRows()
                .stream()
                .map(JobHelper::fromDBRecord)
                .filter(job1 -> job.getId() != job1.getId())
                .collect(Collectors.toList());
    }

    public Job allocate(String userId, String hostIP, JobType jobType, String target, String attachment,
                        long estimatedLoad, boolean immediate) {

        return pivot.allocate(userId, hostIP, jobType, target, attachment, estimatedLoad, immediate);
    }

    public void finish(JobType type, String target) {
        Future<ResultSet> future = $.async(dbClient::queryWithParams, SELECT_ACTIVE_BY_TYPE_AND_TARGET, makeSqlArgument(type, target));
        ResultSet rs = $.await(future);

        Job job = JobHelper.fromDBRecord(SQLHelper.firstRow(rs));
        pivot.finish(job);
    }
}
