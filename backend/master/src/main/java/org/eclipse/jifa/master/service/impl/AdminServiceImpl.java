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
import org.eclipse.jifa.master.entity.Admin;
import org.eclipse.jifa.master.service.AdminService;
import org.eclipse.jifa.master.service.impl.helper.AdminHelper;
import org.eclipse.jifa.master.service.impl.helper.SQLAssert;
import org.eclipse.jifa.master.service.sql.AdminSQL;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;

public class AdminServiceImpl implements AdminService {

    private final JDBCClient dbClient;

    public AdminServiceImpl(JDBCClient dbClient) {
        this.dbClient = dbClient;
    }

    @Override
    public void isAdmin(String userId, Handler<AsyncResult<Boolean>> resultHandler) {
        dbClient.rxQueryWithParams(AdminSQL.SELECT_BY_USER_ID, ja(userId))
                .map(resultSet -> resultSet.getNumRows() == 1)
                .subscribe(SingleHelper.toObserver(resultHandler));
    }

    @Override
    public void add(String userId, Handler<AsyncResult<Void>> handler) {
        dbClient.rxUpdateWithParams(AdminSQL.INSERT, ja(userId))
                .doOnSuccess(SQLAssert::assertUpdated)
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(handler));
    }

    @Override
    public void queryAll(Handler<AsyncResult<List<Admin>>> handler) {
        dbClient.rxQuery(AdminSQL.QUERY_ALL)
                .map(records -> records.getRows().stream().map(AdminHelper::fromDBRecord).collect(Collectors.toList()))
                .subscribe(SingleHelper.toObserver(handler));
    }
}
