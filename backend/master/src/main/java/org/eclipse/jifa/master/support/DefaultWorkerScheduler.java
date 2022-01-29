/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.master.support;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.sql.SQLConnection;
import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.task.DiskCleaningTask;
import org.eclipse.jifa.master.task.DiskUsageUpdatingTask;
import org.eclipse.jifa.master.task.FileSyncTask;
import org.eclipse.jifa.master.task.RetiringTask;
import org.eclipse.jifa.master.task.SchedulingTask;
import org.eclipse.jifa.master.task.TransferJobResultFillingTask;

import java.util.List;
import java.util.Map;

public class DefaultWorkerScheduler implements WorkerScheduler {

    private Pivot pivot;

    @Override
    public void initialize(Pivot pivot, Vertx vertx, JsonObject configs) {
        this.pivot = pivot;
        if (pivot.isLeader()) {
            new DiskCleaningTask(pivot, vertx);
            new RetiringTask(pivot, vertx);
            pivot.setSchedulingTask(new SchedulingTask(pivot, vertx));
            new TransferJobResultFillingTask(pivot, vertx);
            new DiskUsageUpdatingTask(pivot, vertx);
            new FileSyncTask(pivot, vertx);
        }
    }

    @Override
    public Single<Worker> decide(Job job, SQLConnection conn) {
        return pivot.decideWorker(conn, job);
    }

    @Override
    public boolean supportPendingJob() {
        return true;
    }

    @Override
    public Completable start(Job job) {
        return Completable.complete();
    }

    @Override
    public Completable stop(Job job) {
        return Completable.complete();
    }

    @Override
    public Completable stop(Worker worker) {
        throw new JifaException("Unimplemented");
    }

    @Override
    public Single<List<Worker>> list() {
        throw new JifaException("Unimplemented");
    }
}
