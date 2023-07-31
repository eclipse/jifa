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
package org.eclipse.jifa.server.service;

import org.eclipse.jifa.common.domain.exception.ShouldNotReachHereException;
import org.eclipse.jifa.server.domain.dto.HttpRequestToWorker;
import org.eclipse.jifa.server.domain.entity.cluster.WorkerEntity;
import org.eclipse.jifa.server.domain.entity.shared.file.FileEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface WorkerService {

    WorkerEntity resolveForAnalysisApiRequest(FileEntity target);

    <Response> CompletableFuture<Response> sendRequest(WorkerEntity worker, HttpRequestToWorker<Response> request);

    default <Response> Response sendRequestAndBlock(WorkerEntity worker, HttpRequestToWorker<Response> request) {
        try {
            return sendRequest(worker, request).get();
        } catch (Throwable t) {
            if (t instanceof RuntimeException re) {
                throw re;
            }
            if (t instanceof ExecutionException ee) {
                if (ee.getCause() instanceof WebClientResponseException re) {
                    throw re;
                }
            }
            throw new RuntimeException(t);
        }
    }

    default StaticWorkerService asStaticWorkerService() {
        throw new ShouldNotReachHereException();
    }

    default ElasticWorkerService asElasticWorkerService() {
        throw new ShouldNotReachHereException();
    }
}
