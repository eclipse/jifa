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
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.helper.ConfigHelper;
import org.eclipse.jifa.master.service.impl.helper.JobHelper;
import org.eclipse.jifa.master.support.K8SWorkerScheduler;

import java.time.Instant;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.ServiceAssertion.SERVICE_ASSERT;
import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;
import static org.eclipse.jifa.master.service.sql.JobSQL.SELECT_TRANSFER_JOB_TO_FILLING_RESULT;

public class TransferJobResultFillingTask extends BaseTask {
    private static final long MIN_TIMEOUT_THRESHOLD = 5 * 6000;

    private long timeoutThreshold;

    public TransferJobResultFillingTask(Pivot pivot, Vertx vertx) {
        super(pivot, vertx);

    }

    @Override
    public String name() {
        return "Transfer Job Result Filling Task";
    }

    @Override
    public long interval() {
        return ConfigHelper.getLong(pivot.config("JOB-TRANSFER-RESULT-FILLING-INTERVAL"));
    }

    @Override
    public void doInit() {
        timeoutThreshold = ConfigHelper.getLong(pivot.config("JOB-SMALL-TIMEOUT-THRESHOLD"));

        // timeout threshold must be greater than or equal to 5 min
        SERVICE_ASSERT.isTrue(timeoutThreshold >= MIN_TIMEOUT_THRESHOLD, ErrorCode.SANITY_CHECK);
    }

    @Override
    public void doPeriodic() {
        Instant instant = Instant.now().minusMillis(timeoutThreshold);
        pivot.getDbClient().rxQueryWithParams(SELECT_TRANSFER_JOB_TO_FILLING_RESULT, ja(instant))
             .map(result -> result.getRows().stream().map(JobHelper::fromDBRecord).collect(Collectors.toList()))
             .doOnSuccess(jobs -> LOGGER.info("Found timeout file transfer jobs: {}", jobs.size()))
             .doOnSuccess(jobs->{
                 for (Job job : jobs) {
                     pivot.getWorkerScheduler().stopWorker("my-worker" + job.getTarget().hashCode());
                 }
             })
             .map(jobs -> jobs.stream().map(pivot::processTimeoutTransferJob).collect(Collectors.toList()))
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