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

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.helper.ConfigHelper;
import org.eclipse.jifa.master.service.impl.helper.FileHelper;
import org.eclipse.jifa.master.service.impl.helper.WorkerHelper;
import org.eclipse.jifa.master.service.sql.FileSQL;
import org.eclipse.jifa.master.service.sql.JobSQL;
import org.eclipse.jifa.master.service.sql.WorkerSQL;

import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;

public class FileSyncTask extends BaseTask {

    private boolean cleanStale;

    public FileSyncTask(Pivot pivot, Vertx vertx) {
        super(pivot, vertx);
    }

    public boolean isCleanStale() {
        return cleanStale;
    }

    @Override
    public String name() {
        return "File Sync Task";
    }

    @Override
    public long interval() {
        // 1 hour
        return 60 * 60 * 1000;
    }

    @Override
    public void doInit() {
        cleanStale = ConfigHelper.getBoolean(pivot.config("JOB-CLEAN-STALE-FILES"));
    }

    @Override
    public void doPeriodic() {
        pivot.getDbClient().rxQuery(WorkerSQL.SELECT_ALL)
             .map(records -> records.getRows()
                                    .stream()
                                    .map(WorkerHelper::fromDBRecord)
                                    .collect(Collectors.toList()))
             .flatMapCompletable(workers ->
                 Observable.fromIterable(workers)
                     .flatMapCompletable(
                         worker -> pivot.syncFiles(worker, cleanStale)
                             .doOnError(t -> LOGGER.error(
                                 "Failed to sync worker files for {} ", worker.getHostIP(), t))
                             .onErrorComplete()
                     )
             ).andThen(processLongTransferJob()).subscribe(this::end,
                         t -> {
                             LOGGER.error("Execute {} error", name(), t);
                             end();
                         }
        );
    }

    /**
     * Mark files that takes too long time to upload and not recorded in active jobs as ERROR state
     */
    protected Completable processLongTransferJob() {
        JDBCClient dbClient = pivot.getDbClient();
        return dbClient.rxQuery(FileSQL.SELECT_TIMEOUT_IN_PROGRESS_FILE)
            .map(records -> records.getRows()
                .stream()
                .map(FileHelper::fromDBRecord)
                .collect(Collectors.toList()))
            .flatMapCompletable(
                files ->
                    Observable.fromIterable(files)
                        .flatMapCompletable(
                            file -> dbClient.rxQueryWithParams(JobSQL.SELECT_TRANSFER_JOB_BY_NAME, ja(file.getName()))
                                .flatMapCompletable(
                                    rs -> {
                                        if (rs.getRows().size() > 0) {
                                            return Completable.complete();
                                        }
                                        // set state to error if not associated job
                                        return dbClient.rxUpdateWithParams(FileSQL.UPDATE_IN_PROGRESS_FILE_AS_ERROR_BY_NAME, ja(file.getName()))
                                            .ignoreElement();
                                    })
                                .doOnError(t -> LOGGER
                                    .error("Failed to sync file transfer state for {}", file.getName(),
                                        t))
                                .onErrorComplete())
            );
    }
}
