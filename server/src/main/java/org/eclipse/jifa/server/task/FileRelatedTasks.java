/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.condition.ConditionalOnRole;
import org.eclipse.jifa.server.domain.entity.shared.file.TransferringFileEntity;
import org.eclipse.jifa.server.enums.FileTransferState;
import org.eclipse.jifa.server.enums.Role;
import org.eclipse.jifa.server.repository.TransferringFileRepo;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ConditionalOnRole({Role.MASTER, Role.STANDALONE_WORKER})
@Component
public class FileRelatedTasks extends ConfigurationAccessor {

    private final LockSupport lockSupport;

    private final TransferringFileRepo transferringFileRepo;

    private final TaskScheduler taskScheduler;

    public FileRelatedTasks(LockSupport lockSupport, TransferringFileRepo transferringFileRepo, TaskScheduler taskScheduler) {
        this.lockSupport = lockSupport;
        this.transferringFileRepo = transferringFileRepo;
        this.taskScheduler = taskScheduler;
    }

    @Scheduled(initialDelay = 1, fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    void processTimeoutTransferringFiles() {
        lockSupport.runUnderLockIfMaster(() -> {
            List<TransferringFileEntity> files = transferringFileRepo.findAllByLastModifiedTimeBefore(LocalDateTime.now().minusMinutes(5));
            for (TransferringFileEntity file : files) {
                file.setTransferState(FileTransferState.FAILURE);
                file.setFailureMessage("Timeout");
                transferringFileRepo.save(file);
                taskScheduler.schedule(() -> transferringFileRepo.deleteById(file.getId()),
                                       Instant.now().plusSeconds(30));
            }
        }, this.getClass().getSimpleName());
    }
}
