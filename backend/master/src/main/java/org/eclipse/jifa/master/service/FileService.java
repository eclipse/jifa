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
package org.eclipse.jifa.master.service;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.eclipse.jifa.common.enums.FileTransferState;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.master.entity.FileRecord;
import org.eclipse.jifa.master.entity.Job;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.entity.enums.JobType;
import org.eclipse.jifa.master.entity.enums.TransferWay;
import org.eclipse.jifa.master.service.orm.FileRecordHelper;
import org.eclipse.jifa.master.service.orm.SQLAssert;
import org.eclipse.jifa.master.service.orm.SQLHelper;
import org.eclipse.jifa.master.service.sql.FileSQL;
import org.eclipse.jifa.master.service.sql.SQL;
import org.eclipse.jifa.master.support.$;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.orm.SQLHelper.makeSqlArgument;
import static org.eclipse.jifa.master.service.sql.FileSQL.*;

public class FileService extends ServiceCenter {

    private static final long TRANSFER_JOB_LOAD = 5;

    public int count(String userId, FileType type, String expectedFilename) {
        boolean fuzzy = !Strings.isNullOrEmpty(expectedFilename);
        expectedFilename = "%" + expectedFilename + "%";
        String sql = fuzzy ? COUNT_BY_USER_ID_AND_TYPE_AND_EXPECTED_NAME : COUNT_BY_USER_ID_AND_TYPE;
        JsonArray params = fuzzy ? makeSqlArgument(userId, type, expectedFilename, expectedFilename) : makeSqlArgument(userId, type);
        Future<ResultSet> future = $.async(dbClient::queryWithParams, sql, params);
        ResultSet resultSet = $.await(future);
        int count = resultSet.getRows().get(0).getInteger(SQL.COUNT_NAME);
        return count;
    }

    public List<FileRecord> files(String userId, FileType type, String expectedFilename, int page, int pageSize) {
        boolean fuzzy = !Strings.isNullOrEmpty(expectedFilename);
        expectedFilename = "%" + expectedFilename + "%";
        String sql = fuzzy ? SELECT_BY_USER_ID_AND_TYPE_AND_EXPECTED_NAME : SELECT_BY_USER_ID_AND_TYPE;
        JsonArray params = fuzzy ? makeSqlArgument(userId, type, expectedFilename, expectedFilename, (page - 1) * pageSize, pageSize)
                : makeSqlArgument(userId, type, (page - 1) * pageSize, pageSize);
        Future<ResultSet> future = $.async(dbClient::queryWithParams, sql, params);
        ResultSet resultSet = $.await(future);

        return resultSet
                .getRows()
                .stream()
                .map(FileRecordHelper::fromDBRecord)
                .collect(Collectors.toList());
    }

    public FileRecord file(String name) {
        Future<ResultSet> future = $.async(dbClient::queryWithParams, FileSQL.SELECT_FILE_BY_NAME, SQLHelper.makeSqlArgument(name));
        ResultSet resultSet = $.await(future);
        if (resultSet.getRows().size() > 0) {
            return FileRecordHelper.fromDBRecord(resultSet.getRows().get(0));
        }
        return FileRecord.NOT_FOUND;
    }

    public void deleteFile(String name, Deleter deleter) {
        pivot.deleteFile(deleter, name);
    }

    public Job transfer(String userId, FileType type, String originalName, String name, TransferWay transferWay,
                        Map<String, String> transferInfo) {
        FileRecord file = new FileRecord();
        file.setUserId(userId);
        file.setOriginalName(originalName);
        file.setName(name);
        file.setType(type);
        file.setSize(0);
        file.setTransferState(FileTransferState.IN_PROGRESS);
        file.setTransferWay(transferWay);
        file.setTransferInfo(transferInfo);

        boolean immediate = false;

        if (transferWay == TransferWay.OSS ||
                (transferWay == TransferWay.SCP && !Boolean.parseBoolean(transferInfo.get("usePublicKey")))) {
            immediate = true;
        }

        return pivot.allocate(userId, null, JobType.FILE_TRANSFER, name, JSON.toJSONString(file), TRANSFER_JOB_LOAD,
                immediate);
    }

    public void transferDone(String name, FileTransferState transferState, long size) {
        pivot.transferDone(name, transferState, size);
    }

    public void setShared(String name) {
        Future<UpdateResult> future = $.async(dbClient::updateWithParams, FileSQL.SET_SHARED, SQLHelper.makeSqlArgument(name));
        $.await(future);
    }

    public void updateDisplayName(String name, String displayName) {
        Future<UpdateResult> future = $.async(dbClient::updateWithParams, FileSQL.UPDATE_DISPLAY_NAME, SQLHelper.makeSqlArgument(displayName, name));
        UpdateResult updateResult = $.await(future);
        SQLAssert.assertUpdated(updateResult);
    }
}
