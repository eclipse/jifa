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
package org.eclipse.jifa.master.task;

import io.reactivex.Observable;
import io.vertx.reactivex.core.Vertx;
import org.eclipse.jifa.master.entity.File;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.helper.ConfigHelper;
import org.eclipse.jifa.master.service.impl.helper.FileHelper;
import org.eclipse.jifa.master.service.sql.FileSQL;

import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;

/**
 * Periodically clean up dump files that stored at k8s persistent volume claim.
 */
public class PVCCleanupTask extends BaseTask {
    private static final String WORKSPACE = "/root/jifa_workspace";

    public PVCCleanupTask(Pivot pivot, Vertx vertx) {
        super(pivot, vertx);
    }

    @Override
    public String name() {
        return "PVC Cleanup Task";
    }

    @Override
    public long interval() {
        return ConfigHelper.getLong(pivot.config("JOB-DISK-CLEANUP-PERIODIC"));
    }

    @Override
    public void doInit() {
    }

    @Override
    public void doPeriodic() {
        pivot.getDbClient().rxQuery(FileSQL.SELECT_DATED_FILES)
            .map(result -> result.getRows().stream().map(FileHelper::fromDBRecord).collect(Collectors.toList()))
            .doOnSuccess(files -> LOGGER.info("Found dated files for deletion: {}", files.size()))
            .flatMapCompletable(files ->
                Observable.fromIterable(files.stream().map(File::getName).collect(Collectors.toList()))
                    .flatMapCompletable(
                        fileName ->
                            pivot.getDbClient()
                                .rxUpdateWithParams(FileSQL.UPDATE_AS_PENDING_DELETE_BY_FILE_NAME, ja(fileName))
                                .ignoreElement()
                                .andThen(
                                    pivot.getDbClient()
                                        .rxQueryWithParams(FileSQL.SELECT_PENDING_DELETE_BY_FILE_NAME, ja(fileName))
                                        .map(rs -> rs.getRows().stream().map(row -> row.getString("name")).collect(Collectors.toList()))
                                        .flatMapCompletable(fileNames -> pivot
                                            .deleteFile(Deleter.SYSTEM, fileNames.toArray(new String[0]))
                                            .doOnComplete(()->LOGGER.info("Deleted {} files by {}", fileNames.size(), this.name()))
                                        )
                                )
                    )
            )
            .subscribe(() -> {
                    this.end();
                },
                t -> {
                    LOGGER.error("Execute {} error", name(), t);
                    end();
                }
            );
    }
}