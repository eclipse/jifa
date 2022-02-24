/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.helper.ConfigHelper;
import org.eclipse.jifa.master.service.impl.helper.JobHelper;
import org.eclipse.jifa.master.service.sql.JobSQL;
import org.eclipse.jifa.master.support.K8SWorkerScheduler;

import java.util.List;
import java.util.stream.Collectors;

public class StopAbnormalWorkerTask extends BaseTask {
    public StopAbnormalWorkerTask(Pivot pivot, Vertx vertx) {
        super(pivot, vertx);
    }

    @Override
    public String name() {
        return "Stop Abnormal Worker Task";
    }

    @Override
    public long interval() {
        return ConfigHelper.getLong(pivot.config("TASK-STOP-ABNORMAL-WORKER"));
    }

    @Override
    public void doInit() {
    }

    @Override
    public void doPeriodic() {
        pivot.getDbClient().rxQuery(JobSQL.SELECT_ALL_ACTIVE_JOBS)
            .map(result -> result.getRows().stream().map(JobHelper::fromDBRecord).collect(Collectors.toList()))
            .flatMap(jobs -> {
                // Every active job has its own worker, they should never be stopped
                List<String> activeWorkers = jobs.stream().map(job ->
                    pivot.getScheduler().decide(job, null).blockingGet().getHostName()
                ).collect(Collectors.toList());

                // Find all live workers in cloud cluster
                return pivot.getScheduler()
                    .list()
                    .flatMap(workers -> {
                        for (Worker worker : workers) {
                            // Only watch upon normal worker groups, any other special workers have their special lifecycle
                            if (worker.getHostName().startsWith(K8SWorkerScheduler.getNormalWorkerPrefix())) {
                                if (!activeWorkers.contains(worker.getHostName())) {
                                    return pivot.getScheduler().stop(worker).toSingleDefault(worker);
                                }
                            }
                        }
                        return Single.just(Worker.NOT_FOUND);
                    });
            })
            .subscribe(n -> {
                if (n != Worker.NOT_FOUND) {
                    LOGGER.info("Stopped abnormal worker {}/{}", n.getHostName(), n.getHostIP());
                }
                end();
                },
                t -> {
                    LOGGER.error("Execute {} error", name(), t);
                    end();
                }
            );
    }
}
