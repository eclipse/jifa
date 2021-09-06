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
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.service.orm.SQLHelper;
import org.eclipse.jifa.master.service.orm.WorkerHelper;
import org.eclipse.jifa.master.service.sql.FileSQL;
import org.eclipse.jifa.master.service.sql.WorkerSQL;
import org.eclipse.jifa.master.support.$;

import java.util.List;
import java.util.stream.Collectors;

public class WorkerService extends ServiceCenter {

    public List<Worker> queryAll() {
        Future<ResultSet> future = $.async(dbClient::query, WorkerSQL.SELECT_ALL);
        ResultSet resultSet = $.await(future);
        List<Worker> workers = resultSet.getRows()
                .stream()
                .map(WorkerHelper::fromDBRecord)
                .collect(Collectors.toList());
        return workers;
    }

    public void diskCleanup(String hostIP) {
        Future<UpdateResult> future = $.async(dbClient::updateWithParams, FileSQL.UPDATE_AS_PENDING_DELETE, SQLHelper.makeSqlArgument(hostIP));
        $.await(future);

        Future<ResultSet> future1 = $.async(dbClient::queryWithParams, FileSQL.SELECT_PENDING_DELETE, SQLHelper.makeSqlArgument(hostIP));
        ResultSet resultSet = $.await(future1);
        List<String> files = resultSet.getRows().stream().map(row -> row.getString("name"))
                .collect(Collectors.toList());
        pivot.deleteFile(Deleter.ADMIN, files.toArray(new String[0]));
    }

    public Worker selectMostIdleWorker() {
        return pivot.ensureSqlConnection(conn -> {
            return pivot.selectMostIdleWorker(conn);
        });
    }

    public Worker selectWorkerByIP(String hostIp) {
        Future<ResultSet> future = $.async(dbClient::queryWithParams, WorkerSQL.SELECT_BY_IP, SQLHelper.makeSqlArgument(hostIp));
        ResultSet resultSet = $.await(future);
        if (resultSet.getRows().size() > 0) {
            Worker worker = WorkerHelper.fromDBRecord(resultSet.getRows().get(0));
            return worker;
        }
        return Worker.NOT_FOUND;
    }
}
