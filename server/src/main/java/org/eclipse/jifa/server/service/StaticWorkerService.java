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

import org.eclipse.jifa.server.domain.dto.FileTransferRequest;
import org.eclipse.jifa.server.domain.entity.cluster.WorkerEntity;
import org.eclipse.jifa.server.domain.entity.static_cluster.StaticWorkerEntity;
import org.eclipse.jifa.server.enums.FileType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;

public interface StaticWorkerService extends WorkerService {

    StaticWorkerEntity selectForFileTransferRequest(FileTransferRequest request);

    StaticWorkerEntity selectForFileUpload(FileType type, MultipartFile file);

    void handleUploadRequest(WorkerEntity worker, FileType type, MultipartFile file) throws Throwable;

    Resource handleDownloadRequest(WorkerEntity worker, long fileId) throws MalformedURLException;

    @Override
    default StaticWorkerService asStaticWorkerService() {
        return this;
    }
}
