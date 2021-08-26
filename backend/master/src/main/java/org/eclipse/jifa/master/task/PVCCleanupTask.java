/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

import io.vertx.reactivex.core.Vertx;
import org.apache.commons.io.FileUtils;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.helper.ConfigHelper;
import org.eclipse.jifa.master.service.impl.helper.FileHelper;
import org.eclipse.jifa.master.service.sql.FileSQL;

import java.io.File;
import java.util.stream.Collectors;

/**
 * Periodically clean up dump files that stored at k8s persistent volume claim.
 */
public class PVCCleanupTask extends BaseTask {
    private static final String WORKSPACE = "/root/grace_workspace";

    public PVCCleanupTask(Pivot pivot, Vertx vertx) {
        super(pivot, vertx);
    }

    @Override
    public String name() {
        return "PVC Cleanup Task";
    }

    @Override
    public long interval() {
        return ConfigHelper.getLong(pivot.config("JOB-DISK-CLEANUP-PERIODIC"));
    }

    @Override
    public void doInit() {
    }

    @Override
    public void doPeriodic() {
        pivot.getDbClient().rxQuery(FileSQL.SELECT_DATED_FILES)
                .map(result -> result.getRows().stream().map(FileHelper::fromDBRecord).collect(Collectors.toList()))
                .doOnSuccess(files -> LOGGER.info("Found dated files: {}", files.size()))
                .map(files -> {
                    int[] count = new int[2];
                    for (org.eclipse.jifa.master.entity.File virtualFile : files) {
                        String fileType = virtualFile.getType().getTag();
                        String fileName = virtualFile.getName();

                        java.io.File physicalFile = new java.io.File(WORKSPACE + File.separator + fileType + File.separator + fileName);
                        if (physicalFile.exists()) {
                            FileUtils.deleteDirectory(physicalFile);
                            LOGGER.debug("Delete dated file at " + physicalFile.getAbsolutePath());
                            count[0]++;
                        } else {
                            LOGGER.debug("Can not locate dated file at " + physicalFile.getAbsolutePath());
                            count[1]++;
                        }
                    }
                    return count;
                })
                .subscribe(cnt -> LOGGER.info("Total: {}, Clean up:{}, Failed:{}", cnt[0] + cnt[1], cnt[0], cnt[1]),
                        t -> {
                            LOGGER.error("Execute {} error", name(), t);
                            end();
                        }
                );
    }
}