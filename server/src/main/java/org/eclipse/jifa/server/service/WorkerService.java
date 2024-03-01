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
package org.eclipse.jifa.server.service;

import org.eclipse.jifa.server.domain.dto.FileLocation;
import org.eclipse.jifa.server.domain.dto.HttpRequestToWorker;
import org.eclipse.jifa.server.domain.entity.cluster.ElasticWorkerEntity;
import org.eclipse.jifa.server.domain.entity.cluster.StaticWorkerEntity;
import org.eclipse.jifa.server.domain.entity.cluster.WorkerEntity;
import org.eclipse.jifa.server.domain.entity.shared.file.FileEntity;
import org.eclipse.jifa.server.domain.entity.shared.user.UserEntity;
import org.eclipse.jifa.server.enums.ElasticWorkerState;
import org.eclipse.jifa.server.enums.FileType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

public interface WorkerService {

    FileLocation decideLocationForNewFile(UserEntity user, FileType type);

    long forwardUploadRequestToStaticWorker(StaticWorkerEntity worker, FileType type, MultipartFile file) throws Throwable;

    Resource forwardDownloadRequestToStaticWorker(StaticWorkerEntity worker, long fileId) throws Throwable;

    ElasticWorkerState getElasticWorkerState(long workerId);

    ElasticWorkerEntity requestElasticWorkerForAnalysisApiRequest(FileEntity target);

    <Response> Response syncRequest(WorkerEntity worker, HttpRequestToWorker<Response> request);

    <Response> CompletableFuture<Response> asyncRequest(WorkerEntity worker, HttpRequestToWorker<Response> request);
}
