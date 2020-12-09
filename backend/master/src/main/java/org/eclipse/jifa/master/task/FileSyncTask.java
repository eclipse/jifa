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

import io.reactivex.Observable;
import io.vertx.reactivex.core.Vertx;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.helper.ConfigHelper;
import org.eclipse.jifa.master.service.impl.helper.WorkerHelper;
import org.eclipse.jifa.master.service.sql.WorkerSQL;

import java.util.stream.Collectors;

public class FileSyncTask extends BaseTask {

    private boolean cleanStale;

    public FileSyncTask(Pivot pivot, Vertx vertx) {
        super(pivot, vertx);
    }

    @Override
    public String name() {
        return "File Sync Task";
    }

    @Override
    public long interval() {
        // 1 hour
        return 60 * 60 * 1000;
    }

    @Override
    public void doInit() {
        cleanStale = ConfigHelper.getBoolean(pivot.config("JOB-CLEAN-STALE-FILES"));
    }

    @Override
    public void doPeriodic() {
        pivot.getDbClient().rxQuery(WorkerSQL.SELECT_ALL)
             .map(records -> records.getRows()
                                    .stream()
                                    .map(WorkerHelper::fromDBRecord)
                                    .collect(Collectors.toList()))
             .flatMapCompletable(workers -> Observable.fromIterable(workers)
                                                      .flatMapCompletable(
                                                          worker -> pivot.syncFiles(worker, cleanStale)
                                                      )
             ).subscribe(this::end,
                         t -> {
                             LOGGER.error("Execute {} error", name(), t);
                             end();
                         }
        );
    }
}
