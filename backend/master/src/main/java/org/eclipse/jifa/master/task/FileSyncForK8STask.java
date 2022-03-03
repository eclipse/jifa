/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.apache.commons.io.FileUtils;
import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.master.Constant;
import org.eclipse.jifa.master.entity.File;
import org.eclipse.jifa.master.service.impl.Pivot;
import org.eclipse.jifa.master.service.impl.helper.FileHelper;
import org.eclipse.jifa.master.service.sql.FileSQL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Clean up any stale files that does not record in database. The cleanup is
 * exactly different from FileSyncTask, otherwise, they look like the same one.
 */
public class FileSyncForK8STask extends FileSyncTask {
    // TODO: should reuse WorkerGlobal.WORKSPACE and related functionalities
    private static final String WORKSPACE = Constant.DEFAULT_WORKSPACE;

    public FileSyncForK8STask(Pivot pivot, Vertx vertx) {
        super(pivot, vertx);
    }

    private static String dirPath(FileType type) {
        return WORKSPACE + java.io.File.separator + type.getTag();
    }

    private static void delete(FileType type, String name) {
        try {
            java.io.File f = new java.io.File(dirPath(type) + java.io.File.separator + name);
            if (f.isDirectory()) {
                FileUtils.deleteDirectory(f);
            }
        } catch (IOException e) {
            LOGGER.error("Delete file failed", e);
            throw new JifaException(e);
        }
    }

    @Override
    public String name() {
        return "File Sync For K8S Task";
    }

    @Override
    public void doPeriodic() {
        JDBCClient dbClient = pivot.getDbClient();
        dbClient.rxQuery(FileSQL.SELECT_FILES_FOR_SYNC)
            .map(ar -> ar.getRows().stream().map(FileHelper::fromDBRecord).collect(Collectors.toList()))
            .flatMapCompletable(
                files -> {
                    // Clean up any files that exist in workspace while not recorded in database
                    // Merely a mirror of FileSupport.sync
                    Map<FileType, List<String>> filesGroup = new HashMap<>(){{
                        for (FileType ft : FileType.values()) {
                            // In case no files returned
                            this.put(ft, new ArrayList<>());
                        }
                    }};

                    for (File fi : files) {
                        filesGroup.get(fi.getType()).add(fi.getName());
                    }
                    long lastModified = System.currentTimeMillis() - Constant.STALE_THRESHOLD;

                    for (FileType ft : filesGroup.keySet()) {
                        List<String> names = filesGroup.get(ft);
                        java.io.File[] listFiles = new java.io.File(dirPath(ft)).listFiles();
                        if (listFiles == null) {
                            continue;
                        }
                        for (java.io.File lf : listFiles) {
                            if (names.contains(lf.getName())) {
                                continue;
                            }
                            LOGGER.info("{} is not synchronized", lf.getName());
                            if (isCleanStale() && lf.lastModified() < lastModified) {
                                LOGGER.info("Delete stale file {}", lf.getName());
                                delete(ft, lf.getName());
                            }
                        }
                    }
                    return Completable.complete();
                }
            ).andThen(processLongTransferJob())
            .subscribe(this::end,
                t -> {
                    LOGGER.error("Execute {} error", name(), t);
                    end();
                }
            );
    }
}
