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

import io.reactivex.Completable;
import io.vertx.reactivex.core.Vertx;
import org.eclipse.jifa.common.aux.ErrorCode;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.helper.ConfigHelper;
import org.eclipse.jifa.master.service.impl.helper.JobHelper;

import java.time.Instant;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.ServiceAssertion.SERVICE_ASSERT;
import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;
import static org.eclipse.jifa.master.service.sql.JobSQL.SELECT_TO_RETIRE;

/**
 * Retiring timeouted IN_PROGRESS active jobs
 */
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
        Instant instant = Instant.now().minusMillis(timeoutThreshold);
        pivot.getDbClient().rxQueryWithParams(SELECT_TO_RETIRE, ja(instant))
             .map(result -> result.getRows().stream().map(JobHelper::fromDBRecord).collect(Collectors.toList()))
             .doOnSuccess(jobs -> LOGGER.info("Found timeout jobs: {}", jobs.size()))
             .doOnSuccess(jobs -> jobs.forEach(Pivot::buildWorkerName))
             .map(jobs -> jobs.stream().map(pivot::finish).collect(Collectors.toList()))
             .flatMapCompletable(Completable::concat)
             .subscribe(
                 this::end,
                 t -> {
                     LOGGER.error("Execute {} error", name(), t);
                     end();
                 }
             );
    }
}