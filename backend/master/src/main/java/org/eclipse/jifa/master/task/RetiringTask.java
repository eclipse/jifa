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
package org.eclipse.jifa.master.task;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.sql.ResultSet;
import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.service.orm.ConfigHelper;
import org.eclipse.jifa.master.service.orm.JobHelper;
import org.eclipse.jifa.master.service.orm.SQLHelper;
import org.eclipse.jifa.master.support.$;
import org.eclipse.jifa.master.support.Pivot;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.ServiceAssertion.SERVICE_ASSERT;
import static org.eclipse.jifa.master.service.sql.JobSQL.SELECT_TO_RETIRE;

public class RetiringTask extends BaseTask {
    private static long MIN_TIMEOUT_THRESHOLD = 5 * 6000L;

    private static long timeoutThreshold;

    public RetiringTask(Pivot pivot, Vertx vertx) {
        super(pivot, vertx);
    }

    @Override
    public String name() {
        return "Retiring Task";
    }

    @Override
    public long interval() {
        return ConfigHelper.getLong(pivot.config("JOB-RETIRING-INTERVAL"));
    }

    @Override
    public void doInit() {
        timeoutThreshold = ConfigHelper.getLong(pivot.config("JOB-COMMON-TIMEOUT-THRESHOLD"));

        // timeout threshold must be greater than or equal to 5 min
        SERVICE_ASSERT.isTrue(timeoutThreshold >= MIN_TIMEOUT_THRESHOLD, ErrorCode.SANITY_CHECK);
    }

    @Override
    public void doPeriodic() {
        try {
            Instant instant = Instant.now().minusMillis(timeoutThreshold);
            Future<ResultSet> future = $.async(pivot.dbClient()::queryWithParams, SELECT_TO_RETIRE, SQLHelper.makeSqlArgument(instant));
            ResultSet result = $.await(future);
            List<Job> jobs = result.getRows().stream().map(JobHelper::fromDBRecord).collect(Collectors.toList());
            LOGGER.info("Found timeout jobs: {}", jobs.size());
            jobs.forEach(pivot::finish);
            this.end();
        } catch (Throwable e) {
            LOGGER.error("Execute {} error", name(), e);
        }
    }
}