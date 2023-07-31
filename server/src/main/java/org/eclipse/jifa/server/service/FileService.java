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

import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jifa.common.domain.vo.PageView;
import org.eclipse.jifa.server.domain.dto.FileTransferProgress;
import org.eclipse.jifa.server.domain.dto.FileTransferRequest;
import org.eclipse.jifa.server.domain.dto.FileView;
import org.eclipse.jifa.server.domain.entity.shared.file.FileEntity;
import org.eclipse.jifa.server.enums.FileType;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    PageView<FileView> getUserFileViews(FileType type, int page, int pageSize);

    FileView getFileViewById(long fileId);

    void deleteById(long fileId);

    long handleTransferRequest(FileTransferRequest request) throws Throwable;

    FileTransferProgress getTransferProgress(long transferringFileId);

    void handleUploadRequest(FileType type, MultipartFile file) throws Throwable;

    ResponseEntity<Resource> handleDownloadRequest(long fileId, HttpServletResponse response) throws Throwable;

    FileEntity getFileByUniqueName(String uniqueName, FileType expectedFileType);

    void deleteOldestFile();
}
