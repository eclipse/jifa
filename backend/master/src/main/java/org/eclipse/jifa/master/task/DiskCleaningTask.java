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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.service.orm.ConfigHelper;
import org.eclipse.jifa.master.service.orm.SQLHelper;
import org.eclipse.jifa.master.service.sql.ConfigSQL;
import org.eclipse.jifa.master.service.sql.FileSQL;
import org.eclipse.jifa.master.service.sql.WorkerSQL;
import org.eclipse.jifa.master.support.$;
import org.eclipse.jifa.master.support.Pivot;

import java.util.List;
import java.util.stream.Collectors;

public class DiskCleaningTask extends BaseTask {

    public DiskCleaningTask(Pivot pivot, Vertx vertx) {
        super(pivot, vertx);
    }

    /**
     * The logic is straightforward, assume we have a disk cleaning thread named D and
     * a user thread named U, we can use CAS operation to handle both user requesting
     * and system disk cleaning, the pseudocode is as follows:
     * <code>
     * void U(){
     * if(atomic.cmpxchg(in_use,0,1){
     * // using
     * atomic.cmpxchg(in_use,1,0)
     * }
     * }
     * <p>
     * void D(){
     * if(atomic.cmpxchg(in_use,0,2){
     * // deleting
     * atomic.cmpxchg(in_use,2,0);
     * }
     * }
     * </code>
     */
    private static void markAndDeleteFiles(JDBCClient jdbcClient, Pivot pivot,
                                           List<String> workerIpList) {
        for (String workerIp : workerIpList) {
            Future<UpdateResult> future = $.async(jdbcClient::updateWithParams, FileSQL.UPDATE_AS_PENDING_DELETE, SQLHelper.makeSqlArgument(workerIp));
            $.await(future);
            Future<ResultSet> future1 = $.async(jdbcClient::queryWithParams, FileSQL.SELECT_PENDING_DELETE, SQLHelper.makeSqlArgument(workerIp));
            ResultSet rs = $.await(future1);
            List<String> fileNames = rs.getRows().stream()
                    .map(row -> row.getString("name"))
                    .collect(Collectors.toList());
            pivot.deleteFile(Deleter.SYSTEM, fileNames.toArray(new String[0]));

        }
    }

    @Override
    public String name() {
        return "Disk cleaning task";
    }

    @Override
    public long interval() {
        return ConfigHelper.getLong(pivot.config("JOB-DISK-CLEANUP-PERIODIC"));
    }


    @Override
    public void doPeriodic() {
        // Get high dick overload workers, for each of them, get all files which
        // neither deleted nor used in that worker, finally, apply real disk cleanup
        // action for every file in that list
        try {
            if (!isEnableDiskCleaning()) {
                return;
            }
            List<String> workerIpList = getHighDiskOverloadWorkers();
            markAndDeleteFiles(pivot.dbClient(), pivot, workerIpList);
        } catch (Throwable e) {
            LOGGER.error("Execute {} error", name(), e);
        }
    }

    private List<String> getHighDiskOverloadWorkers() {
        Future<ResultSet> future = $.async(pivot.dbClient()::query, WorkerSQL.SELECT_FOR_DISK_CLEANUP);
        ResultSet resultSet = $.await(future);
        return resultSet.getRows()
                .stream()
                .map(jo -> jo.getString("host_ip"))
                .collect(Collectors.toList());
    }

    private boolean isEnableDiskCleaning() {
        Future<ResultSet> future = $.async(pivot.dbClient()::queryWithParams, ConfigSQL.SELECT, SQLHelper.makeSqlArgument("TASK-ENABLE-DISK-CLEANUP"));
        ResultSet resultSet = $.await(future);
        String config = resultSet.getRows().get(0).getString("value");
        long value = Long.parseLong(config);
        return value == 1;
    }
}
