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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.eclipse.jifa.master.entity.UserWorker;
import org.eclipse.jifa.master.service.UserWorkerService;
import org.eclipse.jifa.master.service.impl.helper.SQLAssert;
import org.eclipse.jifa.master.service.impl.helper.SQLHelper;
import org.eclipse.jifa.master.service.impl.helper.UserWorkerHelper;
import org.eclipse.jifa.master.service.sql.UserWorkerSQL;
import org.eclipse.jifa.master.service.sql.WorkerSQL;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.jifa.common.aux.ErrorCode.PRIVATE_HOST_IP;
import static org.eclipse.jifa.common.aux.ErrorCode.REPEATED_USER_WORKER;
import static org.eclipse.jifa.master.service.ServiceAssertion.SERVICE_ASSERT;
import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;

public class UserWorkerServiceImpl implements UserWorkerService, UserWorkerSQL {

    private final JDBCClient dbClient;

    public UserWorkerServiceImpl(JDBCClient dbClient) {
        this.dbClient = dbClient;
    }

    @Override
    public void add(String hostIP, int port, List<String> userIds, String operator,
                    Handler<AsyncResult<Void>> handler) {
        dbClient.rxQueryWithParams(WorkerSQL.SELECT_BY_IP, ja(hostIP))
                .doOnSuccess(record -> SERVICE_ASSERT.isTrue(record.getNumRows() == 0, PRIVATE_HOST_IP))
                .ignoreElement()
                .andThen(dbClient.rxQueryWithParams(SELECT_BY_HOST_IP_AND_PORT, ja(hostIP, port)))
                .doOnSuccess(record -> SERVICE_ASSERT.isTrue(record.getNumRows() == 0, REPEATED_USER_WORKER))
                .ignoreElement()
                .andThen(dbClient
                             .rxUpdateWithParams(INSERT, ja(hostIP, port, JSON.toJSONString(userIds), true, operator)))
                .doOnSuccess(SQLAssert::assertUpdated)
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void delete(String hostIP, int port, Handler<AsyncResult<Void>> handler) {
        dbClient.rxUpdateWithParams(DELETE, ja(hostIP, port))
                .doOnSuccess(SQLAssert::assertUpdated)
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void updateUserIds(String hostIP, int port, List<String> userIds, String operator,
                              Handler<AsyncResult<Void>> handler) {

        dbClient.rxUpdateWithParams(UPDATE_USER_IDS, ja(JSON.toJSONString(userIds), operator, hostIP, port))
                .doOnSuccess(SQLAssert::assertUpdated)
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void enable(String hostIP, int port, String operator, Handler<AsyncResult<Void>> handler) {
        dbClient.rxUpdateWithParams(UPDATE_USER_IDS, ja(true, operator, hostIP, port))
                .doOnSuccess(SQLAssert::assertUpdated)
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void disable(String hostIP, int port, String operator, Handler<AsyncResult<Void>> handler) {
        dbClient.rxUpdateWithParams(UPDATE_USER_IDS, ja(false, operator, hostIP, port))
                .doOnSuccess(SQLAssert::assertUpdated)
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void updateUserIdsAndState(String hostIP, int port, List<String> userIds, boolean enabled, String operator,
                                      Handler<AsyncResult<Void>> handler) {

        dbClient.rxUpdateWithParams(UPDATE_USER_IDS_AND_STATE, ja(JSON.toJSONString(userIds), enabled, operator,
                                                                  hostIP, port))
                .doOnSuccess(SQLAssert::assertUpdated)
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(handler));

    }

    @Override
    public void query(String hostIP, int port, Handler<AsyncResult<UserWorker>> handler) {
        dbClient.rxQueryWithParams(SELECT_BY_HOST_IP_AND_PORT, ja(hostIP, port))
                .map(resultSet -> {
                    if (resultSet.getNumRows() == 0) {
                        return UserWorker.NOT_FOUND;
                    }

                    return UserWorkerHelper.fromDBRecord(SQLHelper.singleRow(resultSet));
                })
                .subscribe(SingleHelper.toObserver(handler));
    }

    @Override
    public void queryAll(Handler<AsyncResult<List<UserWorker>>> handler) {
        dbClient.rxQuery(SELECT_ALL)
                .map(records -> records.getRows()
                                       .stream()
                                       .map(UserWorkerHelper::fromDBRecord)
                                       .collect(Collectors.toList()))
                .subscribe(SingleHelper.toObserver(handler));
    }
}
