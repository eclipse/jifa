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
package org.eclipse.jifa.master.task;

import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.helper.ConfigHelper;
import org.eclipse.jifa.master.service.impl.helper.JobHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.sql.JobSQL.SELECT_ALL_PENDING;

public class SchedulingTask extends BaseTask {
    private int index;

    private List<Job> pendingJobs = new ArrayList<>();

    private Set<String> pinnedHostIPs = new HashSet<>();

    public SchedulingTask(Pivot pivot, Vertx vertx) {
        super(pivot, vertx);
    }

    @Override
    public String name() {
        return "Scheduling Task";
    }

    @Override
    public long interval() {
        return ConfigHelper.getLong(pivot.config("JOB-SCHEDULING-INTERVAL"));
    }

    @Override
    public void doPeriodic() {
        pivot.getDbClient().rxQuery(SELECT_ALL_PENDING)
             .map(
                 records -> records.getRows().stream().map(JobHelper::fromDBRecord).collect(Collectors.toList()))
             .map(jobs -> {
                 pendingJobs.addAll(jobs);
                 LOGGER.info("Found pending jobs: {}", pendingJobs.size());
                 return pendingJobs.size() > 0;
             })
             .subscribe(hasPendingJobs -> {
                 if (hasPendingJobs) {
                     processNextPendingJob();
                 } else {
                     end();
                 }
             }, t -> {
                 LOGGER.error("Execute {} error", name(), t);
                 end();
             });
    }

    @Override
    public void doEnd() {
        index = 0;
        pendingJobs.clear();
        pinnedHostIPs.clear();
    }

    private void processNextPendingJob() {
        if (index < pendingJobs.size()) {
            processJob(pendingJobs.get(index++))
                .subscribe(
                    next -> {
                        if (next) {
                            processNextPendingJob();
                        }
                    },
                    t -> {
                        LOGGER.error("Execute {} error", name(), t);
                        end();
                    }
                );
        } else {
            end();
        }
    }

    private Single<Boolean> processJob(Job job) {
        return job.getHostIP() == null ? processNoBindingJob(job) : processBindingJob(job);
    }

    private Single<Boolean> processNoBindingJob(Job job) {
        return pivot.inTransactionAndLock(
            conn -> pivot.selectMostIdleWorker(conn).flatMap(
                worker -> {
                    long loadSum = worker.getCurrentLoad() + job.getEstimatedLoad();
                    if (loadSum > worker.getMaxLoad()) {
                        return Single.just(false);
                    }
                    String hostIP = worker.getHostIP();
                    job.setHostIP(hostIP);
                    return pivot.updatePendingJobToInProcess(conn, job)
                                .andThen(pivot.updateWorkerLoad(conn, hostIP, loadSum))
                                .toSingleDefault(true);
                }
            )
        ).doOnSuccess(e -> pivot.postInProgressJob(job).subscribe());
    }

    private Single<Boolean> processBindingJob(Job job) {
        return pivot.inTransactionAndLock(
            conn -> pivot.selectWorker(conn, job.getHostIP()).flatMap(
                worker -> {
                    String hostIP = worker.getHostIP();
                    if (pinnedHostIPs.contains(hostIP)) {
                        return Single.just(true);
                    }

                    long loadSum = worker.getCurrentLoad() + job.getEstimatedLoad();
                    if (loadSum > worker.getMaxLoad()) {
                        LOGGER.info("Pin host: {}", hostIP);
                        pinnedHostIPs.add(hostIP);
                        return Single.just(true);
                    }
                    return pivot.updatePendingJobToInProcess(conn, job)
                                .andThen(pivot.updateWorkerLoad(conn, hostIP, loadSum))
                                .toSingleDefault(true);
                }
            )
        ).doOnSuccess(e -> pivot.postInProgressJob(job).subscribe());
    }
}