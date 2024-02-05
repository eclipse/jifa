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

import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.condition.Master;
import org.eclipse.jifa.server.domain.entity.cluster.ElasticWorkerEntity;
import org.eclipse.jifa.server.enums.ElasticWorkerState;
import org.eclipse.jifa.server.repository.ElasticWorkerRepo;
import org.eclipse.jifa.server.service.ElasticWorkerScheduler;
import org.eclipse.jifa.server.service.WorkerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.eclipse.jifa.server.Constant.HTTP_HEALTH_CHECK_MAPPING;
import static org.eclipse.jifa.server.domain.dto.HttpRequestToWorker.createGetRequest;

@Master
@Component
public class ElasticWorkerRelatedTasks extends ConfigurationAccessor {

    private final LockSupport lockSupport;

    private final ElasticWorkerRepo elasticWorkerRepo;

    private final ElasticWorkerScheduler elasticWorkerScheduler;

    private final WorkerService workerService;

    public ElasticWorkerRelatedTasks(LockSupport lockSupport, ElasticWorkerRepo elasticWorkerRepo,
                                     ElasticWorkerScheduler elasticWorkerScheduler,
                                     WorkerService workerService) {
        this.lockSupport = lockSupport;
        this.elasticWorkerRepo = elasticWorkerRepo;
        this.elasticWorkerScheduler = elasticWorkerScheduler;
        this.workerService = workerService;
    }

    @Scheduled(initialDelay = 1, fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    void terminateInconsistentInstances() {
        lockSupport.runUnderLock(elasticWorkerScheduler::terminateInconsistentInstancesQuietly, this.getClass().getSimpleName() + "#terminateInconsistentInstances");
    }

    @Scheduled(initialDelay = 1, fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    void terminateUnhealthyInstances() {
        lockSupport.runUnderLock(() -> {
            List<ElasticWorkerEntity> elasticWorkers = elasticWorkerRepo.findAllByState(ElasticWorkerState.READY);

            elasticWorkers.forEach(elasticWorker -> {
                try {
                    workerService.syncRequest(elasticWorker, createGetRequest(HTTP_HEALTH_CHECK_MAPPING, null, Void.class));
                } catch (Throwable t) {
                    elasticWorkerRepo.deleteById(elasticWorker.getId());
                    try {
                        elasticWorkerScheduler.terminate(elasticWorker.getId());
                    } catch (Exception ignored) {
                    }
                }
            });
        }, this.getClass().getSimpleName() + "#terminateUnhealthyInstances");
    }
}
