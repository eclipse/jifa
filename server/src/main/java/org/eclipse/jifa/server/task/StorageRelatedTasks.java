/********************************************************************************
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.server.task;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.enums.FileType;
import org.eclipse.jifa.server.repository.FileRepo;
import org.eclipse.jifa.server.repository.TransferringFileRepo;
import org.eclipse.jifa.server.service.FileService;
import org.eclipse.jifa.server.service.StorageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class StorageRelatedTasks extends ConfigurationAccessor {

    private final LockSupport lockSupport;

    private final FileRepo fileRepo;

    private final TransferringFileRepo transferringFileRepo;

    private final FileService fileService;

    private final StorageService storageService;

    private static final double THRESHOLD = 0.05;

    public StorageRelatedTasks(LockSupport lockSupport,
                               FileRepo fileRepo,
                               TransferringFileRepo transferringFileRepo,
                               FileService fileService,
                               StorageService storageService) {
        this.lockSupport = lockSupport;
        this.fileRepo = fileRepo;
        this.transferringFileRepo = transferringFileRepo;
        this.fileService = fileService;
        this.storageService = storageService;
    }

    @Scheduled(initialDelay = 1, fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void cleanup() {
        if (!shouldClean()) {
            return;
        }
        lockSupport.runUnderLockIfMaster(() -> {
            do {
                fileService.deleteOldestFile();
            } while (shouldClean());
        }, this.getClass().getSimpleName() + "#cleanup");

    }

    @Scheduled(initialDelay = 10, fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void sync() throws Throwable {
        lockSupport.runUnderLockIfMaster(() -> {
            Map<FileType, Set<String>> map = storageService.getAllFiles();
            for (Map.Entry<FileType, Set<String>> entry : map.entrySet()) {
                FileType type = entry.getKey();
                for (String name : entry.getValue()) {
                    if (transferringFileRepo.findByUniqueName(name).isPresent() ||
                        fileRepo.findByUniqueName(name).isPresent()) {
                        continue;
                    }
                    storageService.scavenge(type, name);
                }
            }
        }, this.getClass().getSimpleName() + "#sync");
    }

    private boolean shouldClean() {
        try {
            long availableSpace = storageService.getAvailableSpace();
            long totalSpace = storageService.getTotalSpace();
            return availableSpace * 1.0 / totalSpace <= THRESHOLD;
        } catch (IOException e) {
            log.error("Failed to get storage space", e);
            return false;
        }
    }
}
