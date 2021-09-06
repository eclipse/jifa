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

import io.vertx.core.Future;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.eclipse.jifa.master.entity.Admin;
import org.eclipse.jifa.master.service.orm.AdminHelper;
import org.eclipse.jifa.master.service.orm.SQLAssert;
import org.eclipse.jifa.master.service.orm.SQLHelper;
import org.eclipse.jifa.master.service.sql.AdminSQL;
import org.eclipse.jifa.master.support.$;

import java.util.List;
import java.util.stream.Collectors;

public final class AdminService extends ServiceCenter {

    public Boolean isAdmin(String userId) {
        Future<ResultSet> future = $.async(dbClient::queryWithParams, AdminSQL.SELECT_BY_USER_ID, SQLHelper.makeSqlArgument(userId));
        ResultSet resultSet = $.await(future);
        return resultSet.getNumRows() == 1;
    }


    public void add(String userId) {
        Future<UpdateResult> future = $.async(dbClient::updateWithParams, AdminSQL.INSERT, SQLHelper.makeSqlArgument(userId));
        UpdateResult rs = $.await(future);
        SQLAssert.assertUpdated(rs);
    }

    public List<Admin> queryAll() {
        Future<ResultSet> future = $.async(dbClient::query, AdminSQL.QUERY_ALL);
        ResultSet rs = $.await(future);
        return rs.getRows().stream().map(AdminHelper::fromDBRecord).collect(Collectors.toList());
    }
}
