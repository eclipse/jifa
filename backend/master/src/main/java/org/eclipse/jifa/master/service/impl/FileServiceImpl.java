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

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.File;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.model.TransferWay;
import org.eclipse.jifa.master.service.FileService;
import org.eclipse.jifa.master.service.impl.helper.FileHelper;
import org.eclipse.jifa.master.service.impl.helper.SQLAssert;
import org.eclipse.jifa.master.service.sql.FileSQL;
import org.eclipse.jifa.master.service.sql.SQL;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;
import static org.eclipse.jifa.master.service.sql.FileSQL.*;

public class FileServiceImpl implements FileService, Constant {

    private static final long TRANSFER_JOB_LOAD = 5;

    private final Pivot pivot;

    private final JDBCClient dbClient;

    public FileServiceImpl(Pivot pivot, JDBCClient dbClient) {
        this.pivot = pivot;
        this.dbClient = dbClient;
    }

    @Override
    public void count(String userId, FileType type, String expectedFilename, Handler<AsyncResult<Integer>> handler) {
        boolean fuzzy = !Strings.isNullOrEmpty(expectedFilename);
        expectedFilename = "%" + expectedFilename + "%";
        String sql = fuzzy ? COUNT_BY_USER_ID_AND_TYPE_AND_EXPECTED_NAME : COUNT_BY_USER_ID_AND_TYPE;
        JsonArray params = fuzzy ? ja(userId, type, expectedFilename, expectedFilename) : ja(userId, type);
        dbClient.rxQueryWithParams(sql, params)
                .map(resultSet -> resultSet.getRows().get(0).getInteger(SQL.COUNT_NAME))
                .subscribe(SingleHelper.toObserver(handler));
    }

    @Override
    public void files(String userId, FileType type, String expectedFilename, int page, int pageSize,
                      Handler<AsyncResult<List<File>>> handler) {
        boolean fuzzy = !Strings.isNullOrEmpty(expectedFilename);
        expectedFilename = "%" + expectedFilename + "%";
        String sql = fuzzy ? SELECT_BY_USER_ID_AND_TYPE_AND_EXPECTED_NAME : SELECT_BY_USER_ID_AND_TYPE;
        JsonArray params = fuzzy ? ja(userId, type, expectedFilename, expectedFilename, (page - 1) * pageSize, pageSize)
                                 : ja(userId, type, (page - 1) * pageSize, pageSize);
        dbClient.rxQueryWithParams(sql, params)
                .map(ar -> ar.getRows().stream().map(FileHelper::fromDBRecord).collect(Collectors.toList()))
                .subscribe(SingleHelper.toObserver(handler));
    }

    @Override
    public void file(String name, Handler<AsyncResult<File>> handler) {
        dbClient.rxQueryWithParams(FileSQL.SELECT_FILE_BY_NAME, ja(name))
                .map(ar -> {
                    if (ar.getRows().size() > 0) {
                        return FileHelper.fromDBRecord(ar.getRows().get(0));
                    }
                    return File.NOT_FOUND;
                })
                .subscribe(SingleHelper.toObserver(handler));
    }

    @Override
    public void deleteFile(String name, Deleter deleter, Handler<AsyncResult<Void>> handler) {
        pivot.deleteFile(deleter, name)
             .subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void transfer(String userId, FileType type, String originalName, String name, TransferWay transferWay,
                         Map<String, String> transferInfo, Handler<AsyncResult<Job>> handler) {
        File file = new File();
        file.setUserId(userId);
        file.setOriginalName(originalName);
        file.setName(name);
        file.setType(type);
        file.setSize(0);
        file.setTransferState(FileTransferState.IN_PROGRESS);
        file.setTransferWay(transferWay);
        file.setTransferInfo(transferInfo);

        String hostIp = pivot.getWorkerScheduler().getWorkerInfo(transferInfo.get("workerName")).getIp();
        boolean immediate = false;

        if (transferWay == TransferWay.OSS ||
            (transferWay == TransferWay.SCP && !Boolean.parseBoolean(transferInfo.get("usePublicKey")))) {
            immediate = true;
        }

        pivot.allocate(userId, hostIp, JobType.FILE_TRANSFER, name, JSON.toJSONString(file), TRANSFER_JOB_LOAD,
                       immediate)
             .subscribe(SingleHelper.toObserver(handler));
    }

    @Override
    public void transferDone(String name, FileTransferState transferState, long size,
                             Handler<AsyncResult<Void>> handler) {
        pivot.transferDone(name, transferState, size).subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void setShared(String name, Handler<AsyncResult<Void>> handler) {
        dbClient.rxUpdateWithParams(FileSQL.SET_SHARED, ja(name))
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void updateDisplayName(String name, String displayName, Handler<AsyncResult<Void>> handler) {
        dbClient.rxUpdateWithParams(FileSQL.UPDATE_DISPLAY_NAME, ja(displayName, name))
                .doOnSuccess(SQLAssert::assertUpdated)
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(handler));
    }
}
