/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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
import io.vertx.ext.sql.ResultSet;
import org.eclipse.jifa.master.entity.Worker;
import org.eclipse.jifa.master.service.orm.ConfigHelper;
import org.eclipse.jifa.master.service.orm.WorkerHelper;
import org.eclipse.jifa.master.service.sql.WorkerSQL;
import org.eclipse.jifa.master.support.$;
import org.eclipse.jifa.master.support.Pivot;

import java.util.List;
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
        try {
            Future<ResultSet> future = $.async(pivot.dbClient()::query, WorkerSQL.SELECT_ALL);
            ResultSet resultSet = $.await(future);
            List<Worker> workers = resultSet.getRows().stream().map(WorkerHelper::fromDBRecord).collect(Collectors.toList());
            for (Worker worker : workers) {
                pivot.syncFiles(worker, cleanStale);
            }
            this.end();
        } catch (Throwable e) {
            LOGGER.error("Execute {} error", name(), e);

        }
    }
}
