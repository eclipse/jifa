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
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.eclipse.jifa.master.entity.enums.Deleter;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.helper.ConfigHelper;
import org.eclipse.jifa.master.service.sql.ConfigSQL;
import org.eclipse.jifa.master.service.sql.FileSQL;
import org.eclipse.jifa.master.service.sql.WorkerSQL;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.jifa.master.service.impl.helper.SQLHelper.ja;

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
    private static Observable<Completable> markAndDeleteFiles(JDBCClient jdbcClient, Pivot pivot,
                                                              List<String> workerIpList) {
        return Observable.fromIterable(workerIpList)
                         .flatMap(
                             workerIp -> jdbcClient.rxUpdateWithParams(FileSQL.UPDATE_AS_PENDING_DELETE_BY_HOST, ja(workerIp))
                                                   .ignoreElement()
                                                   .andThen(jdbcClient.rxQueryWithParams(FileSQL.SELECT_PENDING_DELETE_BY_HOST,
                                                                                         ja(workerIp))
                                                                      .map(rs -> rs.getRows().stream()
                                                                                   .map(row -> row.getString("name"))
                                                                                   .collect(Collectors.toList()))
                                                                      .flatMapCompletable(fileNames -> pivot
                                                                          .deleteFile(Deleter.SYSTEM,
                                                                                      fileNames.toArray(new String[0])))
                                                   )
                                                   .toObservable()
                         );
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
        isEnableDiskCleaning()
            .subscribe(val -> getHighDiskOverloadWorkers()
                .flatMapObservable(workerIpList -> markAndDeleteFiles(pivot.getDbClient(), pivot, workerIpList))
                .ignoreElements()
                .subscribe(
                    () -> {
                        LOGGER.info("Execute {} successfully ", name());
                        this.end();
                    },
                    t -> {
                        LOGGER.error("Execute {} error", name(), t);
                        this.end();
                    }
                )
            );
    }

    private Maybe<List<String>> getHighDiskOverloadWorkers() {
        return pivot.getDbClient().rxQuery(WorkerSQL.SELECT_FOR_DISK_CLEANUP)
                    .map(rs -> rs.getRows().stream().map(jo -> jo.getString("host_ip")).collect(Collectors.toList()))
                    .filter(workers -> workers.size() > 0);
    }

    private Maybe<Long> isEnableDiskCleaning() {
        return pivot.getDbClient()
                    .rxQueryWithParams(ConfigSQL.SELECT, ja("TASK-ENABLE-DISK-CLEANUP"))
                    .map(resultSet -> resultSet.getRows().get(0).getString("value"))
                    .map(Long::valueOf)
                    .filter(value -> value == 1);
    }
}
