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
package org.eclipse.jifa.master.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.service.WorkerService;
import org.eclipse.jifa.master.service.impl.helper.WorkerHelper;
import org.eclipse.jifa.master.service.sql.FileSQL;
import org.eclipse.jifa.master.service.sql.WorkerSQL;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;

public class WorkerServiceImpl implements WorkerService, WorkerSQL {

    private final JDBCClient dbClient;

    private final Pivot pivot;

    public WorkerServiceImpl(JDBCClient dbClient, Pivot pivot) {
        this.dbClient = dbClient;
        this.pivot = pivot;
    }

    @Override
    public void queryAll(Handler<AsyncResult<List<Worker>>> handler) {
        dbClient.rxQuery(WorkerSQL.SELECT_ALL)
                .map(records -> records.getRows()
                                       .stream()
                                       .map(WorkerHelper::fromDBRecord)
                                       .collect(Collectors.toList()))
                .subscribe(SingleHelper.toObserver(handler));
    }

    @Override
    public void diskCleanup(String hostIP, Handler<AsyncResult<Void>> handler) {
        dbClient.rxUpdateWithParams(FileSQL.UPDATE_AS_PENDING_DELETE_BY_HOST, ja(hostIP))
                .ignoreElement()
                .andThen(dbClient.rxQueryWithParams(FileSQL.SELECT_PENDING_DELETE_BY_HOST, ja(hostIP))
                                 .map(rs -> rs.getRows().stream().map(row -> row.getString("name"))
                                              .collect(Collectors.toList()))
                                 .flatMapCompletable(
                                     fileNames -> pivot.deleteFile(Deleter.ADMIN, fileNames.toArray(new String[0])))
                )
                .subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void selectMostIdleWorker(Handler<AsyncResult<Worker>> handler) {
        dbClient.rxGetConnection()
                .flatMap(conn -> pivot.selectMostIdleWorker(conn).doOnTerminate(conn::close))
                .subscribe(SingleHelper.toObserver(handler));
    }

    @Override
    public void selectWorkerByIP(String hostIp, Handler<AsyncResult<Worker>> handler) {
        dbClient.rxQueryWithParams(WorkerSQL.SELECT_BY_IP, ja(hostIp))
                .map(ar -> {
                    if (ar.getRows().size() > 0) {
                        return WorkerHelper.fromDBRecord(ar.getRows().get(0));
                    }
                    return Worker.NOT_FOUND;
                })
                .subscribe(SingleHelper.toObserver(handler));
    }
}
