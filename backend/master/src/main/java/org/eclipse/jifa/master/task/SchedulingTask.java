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
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.service.orm.ConfigHelper;
import org.eclipse.jifa.master.service.orm.JobHelper;
import org.eclipse.jifa.master.support.$;
import org.eclipse.jifa.master.support.Pivot;

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
        try {
            Future<ResultSet> future = $.async(pivot.dbClient()::query, SELECT_ALL_PENDING);
            ResultSet resultSet = $.await(future);
            List<Job> jobs = resultSet.getRows().stream().map(JobHelper::fromDBRecord).collect(Collectors.toList());
            pendingJobs.addAll(jobs);
            LOGGER.info("Found pending jobs: {}", pendingJobs.size());
            if (pendingJobs.size() > 0) {
                processNextPendingJob();
            } else {
                end();
            }
        } catch (Throwable e) {
            LOGGER.error("Execute {} error", name(), e);
            end();
        }

    }

    @Override
    public void doEnd() {
        index = 0;
        pendingJobs.clear();
        pinnedHostIPs.clear();
    }

    private void processNextPendingJob() {
        if (index < pendingJobs.size()) {
            try {
                boolean ret = processJob(pendingJobs.get(index++));
                if (ret) {
                    processNextPendingJob();
                }
            } catch (Throwable e) {
                LOGGER.error("Execute {} error", name(), e);
                end();
            }
        } else {
            end();
        }
    }

    private boolean processJob(Job job) {
        return job.getHostIP() == null ? processNoBindingJob(job) : processBindingJob(job);
    }

    private boolean processNoBindingJob(Job job) {
        return pivot.ensureTransaction(conn -> {
            Worker worker = pivot.selectMostIdleWorker(conn);
            long loadSum = worker.getCurrentLoad() + job.getEstimatedLoad();
            if (loadSum > worker.getMaxLoad()) {
                return false;
            }
            String hostIP = worker.getHostIP();
            job.setHostIP(hostIP);
            pivot.updatePendingJobToInProcess(conn, job);
            pivot.updateWorkerLoad(conn, hostIP, loadSum);

            pivot.postInProgressJob(job);
            return true;
        }, err -> LOGGER.error("Can not process binding job {}", job));
    }

    private boolean processBindingJob(Job job) {
        return pivot.ensureTransaction(conn -> {
            pivot.globalLock(conn);
            Worker worker = pivot.selectWorker(conn, job.getHostIP());

            String hostIP = worker.getHostIP();
            if (pinnedHostIPs.contains(hostIP)) {
                return true;
            }

            long loadSum = worker.getCurrentLoad() + job.getEstimatedLoad();
            if (loadSum > worker.getMaxLoad()) {
                LOGGER.info("Pin host: {}", hostIP);
                pinnedHostIPs.add(hostIP);
                return true;
            }
            pivot.updatePendingJobToInProcess(conn, job);
            pivot.updateWorkerLoad(conn, hostIP, loadSum);

            pivot.postInProgressJob(job);
            return true;
        }, err -> LOGGER.error("Can not process binding job {}", job));
    }
}