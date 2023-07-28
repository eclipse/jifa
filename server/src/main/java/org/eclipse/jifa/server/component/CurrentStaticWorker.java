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
package org.eclipse.jifa.server.component;

import jakarta.annotation.PostConstruct;
import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.condition.StaticWorker;
import org.eclipse.jifa.server.domain.entity.static_cluster.StaticWorkerEntity;
import org.eclipse.jifa.server.repository.StaticWorkerRepo;
import org.eclipse.jifa.server.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;

@StaticWorker
@Component
public class CurrentStaticWorker extends ConfigurationAccessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final StaticWorkerRepo staticWorkerRepo;

    private final StorageService storageService;

    private final TaskScheduler taskScheduler;

    private StaticWorkerEntity current;

    public CurrentStaticWorker(StaticWorkerRepo staticWorkerRepo,
                               StorageService storageService,
                               TaskScheduler taskScheduler) {
        this.staticWorkerRepo = staticWorkerRepo;
        this.storageService = storageService;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    private void init() throws IOException {
        InetAddress localHost = InetAddress.getLocalHost();
        String hostAddress = localHost.getHostAddress();
        current = this.staticWorkerRepo.findByHostAddress(hostAddress).orElseGet(() -> {
            StaticWorkerEntity worker = new StaticWorkerEntity();
            worker.setHostAddress(hostAddress);
            return worker;
        });
        current.setPort(config.getPort());
        updateStorageSpace();

        taskScheduler.scheduleAtFixedRate(() -> {
            try {
                updateStorageSpace();
            } catch (Throwable t) {
                LOGGER.warn("Failed to update static worker space value", t);
            }
        }, Instant.now().plusSeconds(60), Duration.ofSeconds(60));
    }

    public StaticWorkerEntity getEntity() {
        return current;
    }

    private void updateStorageSpace() throws IOException {
        current.setAvailableSpace(storageService.getAvailableSpace());
        current.setTotalSpace(storageService.getTotalSpace());
        current = staticWorkerRepo.save(current);
    }
}
