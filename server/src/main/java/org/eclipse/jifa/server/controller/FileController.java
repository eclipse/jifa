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
package org.eclipse.jifa.server.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.eclipse.jifa.common.domain.vo.PageView;
import org.eclipse.jifa.server.domain.dto.FileTransferProgress;
import org.eclipse.jifa.server.domain.dto.FileTransferRequest;
import org.eclipse.jifa.server.domain.dto.FileView;
import org.eclipse.jifa.server.enums.FileType;
import org.eclipse.jifa.server.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/files")
    public PageView<FileView> files(@RequestParam FileType type, @RequestParam int page, @RequestParam int pageSize) {
        return fileService.getUserFileViews(type, page, pageSize);
    }

    @GetMapping("/files/{file-id}")
    public FileView file(@PathVariable("file-id") long fileId) {
        return fileService.getFileViewById(fileId);
    }

    @DeleteMapping("/files/{file-id}")
    public void delete(@PathVariable("file-id") long fileId) {
        fileService.deleteById(fileId);
    }

    @PostMapping("/files/transfer")
    public long transfer(@Valid @RequestBody FileTransferRequest request) throws Throwable {
        return fileService.handleTransferRequest(request);
    }

    @GetMapping("/files/transfer/{transferring-file-id}")
    public FileTransferProgress transferProgress(@PathVariable("transferring-file-id") long transferringFileId) {
        return fileService.getTransferProgress(transferringFileId);
    }

    @PostMapping(value = "/files/upload")
    public void upload(@RequestParam FileType type, @RequestParam MultipartFile file) throws Throwable {
        fileService.handleUploadRequest(type, file);
    }

    @GetMapping("/files/{file-id}/download")
    public ResponseEntity<Resource> download(HttpServletResponse response,
                                             @PathVariable("file-id") long fileId) throws Throwable {
        return fileService.handleDownloadRequest(fileId, response);
    }
}
