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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.client.HttpResponse;
import org.eclipse.jifa.common.vo.DiskUsage;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.service.orm.ConfigHelper;
import org.eclipse.jifa.master.service.orm.WorkerHelper;
import org.eclipse.jifa.master.service.sql.WorkerSQL;
import org.eclipse.jifa.master.support.$;
import org.eclipse.jifa.master.support.Pivot;
import org.eclipse.jifa.master.support.TriConsumer;
import org.eclipse.jifa.master.support.WorkerClient;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;
import static org.eclipse.jifa.master.Constant.uri;
import static org.eclipse.jifa.master.service.orm.SQLHelper.makeSqlArgument;

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
        try {
            List<Worker> workers = getWorkers();
            for (Worker worker : workers) {
                Future<HttpResponse<Buffer>> future = $.asyncVoid(new TriConsumer<String, String, Handler<AsyncResult<HttpResponse<Buffer>>>>() {
                    @Override
                    public void accept(String s, String s2, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
                        WorkerClient.get(s, s2, handler);
                    }
                }, worker.getHostIP(), uri(Constant.SYSTEM_DISK_USAGE));
                HttpResponse<Buffer> resp = $.await(future);
                ASSERT
                        .isTrue(resp.bodyAsJson(DiskUsage.class) != null);
                DiskUsage usage = resp.bodyAsJson(DiskUsage.class);
                updateWorkerDiskUsage(worker.getHostIP(), usage.getTotalSpaceInMb(), usage.getUsedSpaceInMb());
            }
            this.end();
        } catch (Throwable e) {
            LOGGER.error("Execute {} error", name(), e);
        }
    }

    private void updateWorkerDiskUsage(String hostIP, long totalSpaceInMb, long usedSpaceInMb) {
        Future<UpdateResult> future = $.async(pivot.dbClient()::updateWithParams, WorkerSQL.UPDATE_DISK_USAGE, makeSqlArgument(totalSpaceInMb, usedSpaceInMb, hostIP));

        UpdateResult updateResult = $.await(future);
        ASSERT.isTrue(updateResult.getUpdated() == 1);
    }

    private List<Worker> getWorkers() {
        Future<ResultSet> future = $.async(pivot.dbClient()::query, WorkerSQL.SELECT_ALL);
        ResultSet resultSet = $.await(future);
        return resultSet.getRows().stream().map(WorkerHelper::fromDBRecord).collect(Collectors.toList());
    }
}
