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

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.Vertx;
import org.eclipse.jifa.common.vo.DiskUsage;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.helper.ConfigHelper;
import org.eclipse.jifa.master.service.impl.helper.WorkerHelper;
import org.eclipse.jifa.master.service.sql.WorkerSQL;
import org.eclipse.jifa.master.support.WorkerClient;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;
import static org.eclipse.jifa.common.util.GsonHolder.GSON;
import static org.eclipse.jifa.master.Constant.uri;
import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;

public class DiskUsageUpdatingTask extends BaseTask {
    public DiskUsageUpdatingTask(Pivot pivot, Vertx vertx) {
        super(pivot, vertx);
    }

    @Override
    public String name() {
        return "Worker disk usage updating task";
    }

    @Override
    public long interval() {
        return ConfigHelper.getLong(pivot.config("JOB-DISK-USAGE-UPDATING-PERIODIC"));
    }

    @Override
    public void doPeriodic() {
        getWorkers().flatMapObservable(
            workerList -> Observable.fromIterable(workerList)
                                    .flatMapSingle(
                                        worker -> WorkerClient.get(worker.getHostIP(), uri(Constant.SYSTEM_DISK_USAGE))
                                                              .map(resp -> GSON.fromJson(resp.bodyAsString(), DiskUsage.class))
                                                              .flatMap(usage -> updateWorkerDiskUsage(worker.getHostIP(),
                                                                                                      usage.getTotalSpaceInMb(),
                                                                                                      usage.getUsedSpaceInMb()))
                                    )
        ).ignoreElements().subscribe(this::end, t -> {
            LOGGER.error("Execute {} error", name(), t);
            end();
        });
    }

    private Single<UpdateResult> updateWorkerDiskUsage(String hostIP, long totalSpaceInMb, long usedSpaceInMb) {
        return pivot.getDbClient()
                    .rxUpdateWithParams(WorkerSQL.UPDATE_DISK_USAGE, ja(totalSpaceInMb, usedSpaceInMb, hostIP))
                    .doOnSuccess(updateResult -> ASSERT.isTrue(updateResult.getUpdated() == 1));
    }

    private Single<List<Worker>> getWorkers() {
        return pivot.getDbClient()
                    .rxQuery(WorkerSQL.SELECT_ALL)
                    .map(records ->
                             records.getRows().stream().map(WorkerHelper::fromDBRecord).collect(Collectors.toList())
                    );

    }
}
