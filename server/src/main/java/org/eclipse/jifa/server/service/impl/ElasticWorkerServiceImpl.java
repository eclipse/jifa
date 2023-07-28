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
package org.eclipse.jifa.server.service.impl;

import org.eclipse.jifa.server.condition.ElasticSchedulingStrategy;
import org.eclipse.jifa.server.condition.Master;
import org.eclipse.jifa.server.domain.entity.cluster.WorkerEntity;
import org.eclipse.jifa.server.domain.entity.elsatic_cluster.ElasticWorkerEntity;
import org.eclipse.jifa.server.domain.entity.shared.FileEntity;
import org.eclipse.jifa.server.domain.entity.shared.TransferringFileEntity;
import org.eclipse.jifa.server.domain.exception.ElasticWorkerNotReadyException;
import org.eclipse.jifa.server.enums.ElasticWorkerPurpose;
import org.eclipse.jifa.server.enums.ElasticWorkerState;
import org.eclipse.jifa.server.repository.ElasticWorkerRepo;
import org.eclipse.jifa.server.service.ElasticWorkerScheduler;
import org.eclipse.jifa.server.service.ElasticWorkerService;
import org.eclipse.jifa.server.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.UniformRandomBackOffPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import static org.eclipse.jifa.common.domain.exception.CommonException.CE;
import static org.eclipse.jifa.common.enums.CommonErrorCode.INTERNAL_ERROR;
import static org.eclipse.jifa.server.enums.ElasticWorkerPurpose.FILE_ANALYSIS;

@Master
@ElasticSchedulingStrategy
@Service
public class ElasticWorkerServiceImpl extends AbstractWorkerServiceImpl implements ElasticWorkerService {

    private static final int DELETION_DELAY = 60;

    private final ElasticWorkerRepo elasticWorkerRepo;

    private final ElasticWorkerScheduler elasticWorkerScheduler;

    private final TaskScheduler taskScheduler;

    private final RetryTemplate retryTemplateForAcquiringElasticWorker;

    public ElasticWorkerServiceImpl(UserService userService,
                                    ElasticWorkerRepo elasticWorkerRepo,
                                    ElasticWorkerScheduler elasticWorkerScheduler,
                                    TaskScheduler taskScheduler) {
        super(userService);
        this.elasticWorkerRepo = elasticWorkerRepo;
        this.elasticWorkerScheduler = elasticWorkerScheduler;
        this.taskScheduler = taskScheduler;

        retryTemplateForAcquiringElasticWorker = new RetryTemplate();

        CompositeRetryPolicy compositeRetryPolicy = new CompositeRetryPolicy();

        TimeoutRetryPolicy timeoutRetryPolicy = new TimeoutRetryPolicy();
        timeoutRetryPolicy.setTimeout(30000);

        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(Integer.MAX_VALUE,
                                                                    Collections.singletonMap(DataIntegrityViolationException.class, true));

        compositeRetryPolicy.setPolicies(new RetryPolicy[]{
                timeoutRetryPolicy, simpleRetryPolicy
        });

        retryTemplateForAcquiringElasticWorker.setRetryPolicy(compositeRetryPolicy);
        UniformRandomBackOffPolicy backOffPolicy = new UniformRandomBackOffPolicy();
        backOffPolicy.setMinBackOffPeriod(500);
        backOffPolicy.setMaxBackOffPeriod(5000);
        retryTemplateForAcquiringElasticWorker.setBackOffPolicy(new UniformRandomBackOffPolicy());
    }

    @Override
    public WorkerEntity resolveForAnalysisApiRequest(FileEntity target) {
        ElasticWorkerEntity worker = acquireForAnalysis(target);
        return switch (worker.getState()) {
            case READY -> worker;
            case STARTING -> throw new ElasticWorkerNotReadyException(worker.getId());
            case FAILURE -> throw CE(INTERNAL_ERROR);
        };
    }

    @Override
    public ElasticWorkerState getState(long workerId) {
        return elasticWorkerRepo.findById(workerId).orElseThrow(() -> CE(INTERNAL_ERROR))
                                .getState();
    }

    private ElasticWorkerEntity acquireForAnalysis(FileEntity target) {
        return acquire(FILE_ANALYSIS, target.getId(), () -> {
            long GB = 1024 * 1024 * 1024L;
            return (long) Math.max(GB, 1.3 * target.getSize());
        });
    }

    @SuppressWarnings("SameParameterValue")
    private ElasticWorkerEntity acquire(ElasticWorkerPurpose purpose, long referenceId, Supplier<Long> requestedMemorySizeSupplier) {
        return retryTemplateForAcquiringElasticWorker.execute(c -> {

            Optional<ElasticWorkerEntity> optional = elasticWorkerRepo.findByPurposeAndReferenceId(purpose, referenceId);

            if (optional.isPresent()) {
                return optional.get();
            }

            ElasticWorkerEntity elasticWorker = createNewElasticWorker(purpose, referenceId);

            long requestedMemorySize = requestedMemorySizeSupplier.get();

            elasticWorkerScheduler.scheduleAsync(elasticWorker.getId(), requestedMemorySize, (hostAddress, throwable) -> {
                if (throwable != null) {
                    try {
                        elasticWorker.setState(ElasticWorkerState.FAILURE);
                        String failureMessage = throwable.getMessage();
                        if (failureMessage.length() > TransferringFileEntity.MAX_FAILURE_MESSAGE_LENGTH) {
                            failureMessage = failureMessage.substring(0, TransferringFileEntity.MAX_FAILURE_MESSAGE_LENGTH);
                        }
                        elasticWorker.setFailureMessage(failureMessage);
                        elasticWorkerRepo.save(elasticWorker);
                    } finally {
                        taskScheduler.schedule(() -> elasticWorkerRepo.deleteById(elasticWorker.getId()),
                                               Instant.now().plusSeconds(DELETION_DELAY));
                    }
                } else {
                    elasticWorker.setState(ElasticWorkerState.READY);
                    elasticWorkerRepo.save(elasticWorker);
                }
            });
            return elasticWorker;
        });
    }

    private ElasticWorkerEntity createNewElasticWorker(ElasticWorkerPurpose purpose, long referenceId) {
        ElasticWorkerEntity newElasticWorker = new ElasticWorkerEntity();
        newElasticWorker.setPurpose(purpose);
        newElasticWorker.setReferenceId(referenceId);
        newElasticWorker.setState(ElasticWorkerState.STARTING);
        newElasticWorker.setPort(config.getElasticWorkerPort());
        return elasticWorkerRepo.save(newElasticWorker);
    }
}
