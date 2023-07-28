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

import org.eclipse.jifa.server.ConfigurationAccessor;
import org.eclipse.jifa.server.Constant;
import org.eclipse.jifa.server.condition.ElasticWorker;
import org.eclipse.jifa.server.repository.ElasticWorkerRepo;
import org.eclipse.jifa.server.service.ElasticWorkerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ElasticWorker
@Component
public class CurrentElasticWorker extends ConfigurationAccessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ElasticWorkerRepo elasticWorkerRepo;

    private final AtomicInteger activeCount = new AtomicInteger();

    private volatile long lastAccessTime;

    private final long identity;

    private final ElasticWorkerScheduler elasticWorkerScheduler;

    public CurrentElasticWorker(ElasticWorkerRepo elasticWorkerRepo, ElasticWorkerScheduler elasticWorkerScheduler) {
        this.elasticWorkerRepo = elasticWorkerRepo;
        this.elasticWorkerScheduler = elasticWorkerScheduler;
        identity = Long.parseLong(System.getenv(Constant.ELASTIC_WORKER_IDENTITY_ENV_KEY));
    }

    public void preventTermination() {
        activeCount.incrementAndGet();
    }

    public void revokePreventingTermination() {
        lastAccessTime = System.currentTimeMillis();
        int count = activeCount.decrementAndGet();
        if (count < 0) {
            LOGGER.error("activeCount is negative: {}", activeCount.get());
        }
    }

    @Scheduled(initialDelay = 5, fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void terminateIfIdle() {
        if (activeCount.get() > 0) {
            return;
        }
        long delta = System.currentTimeMillis() - lastAccessTime;
        long thresholdInMillis = TimeUnit.MINUTES.toMillis(config.getElasticWorkerIdleThreshold());
        if (delta > thresholdInMillis) {
            try {
                elasticWorkerRepo.deleteById(identity);
                elasticWorkerScheduler.terminate(identity);
            } catch (Throwable t) {
                LOGGER.error("Failed to terminate this elastic worker", t);
            }
        } else {
            LOGGER.info("This elastic worker is currently idle and will be deleted if still not accessed by {}", new Date(lastAccessTime + thresholdInMillis));
        }
    }
}
