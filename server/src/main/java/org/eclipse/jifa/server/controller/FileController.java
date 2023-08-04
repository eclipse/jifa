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
import org.eclipse.jifa.server.domain.dto.WrappedResource;
import org.eclipse.jifa.server.enums.FileType;
import org.eclipse.jifa.server.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * File controller
 */
@RestController
public class FileController {

    private final FileService fileService;

    /**
     * @param fileService file service
     */
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Query the files of the current user by type and paging information.
     *
     * @param type     the expected file type
     * @param page     the page number, starts from 1
     * @param pageSize the page size
     * @return the page view of the files
     */
    @GetMapping("/files")
    public PageView<FileView> files(@RequestParam FileType type, @RequestParam int page, @RequestParam int pageSize) {
        return fileService.getUserFileViews(type, page, pageSize);
    }

    /**
     * Query the file by id.
     *
     * @param fileId the file id
     * @return the file view
     */
    @GetMapping("/files/{file-id}")
    public FileView file(@PathVariable("file-id") long fileId) {
        return fileService.getFileViewById(fileId);
    }

    /**
     * Delete the file by id.
     *
     * @param fileId the file id
     */
    @DeleteMapping("/files/{file-id}")
    public void delete(@PathVariable("file-id") long fileId) {
        fileService.deleteById(fileId);
    }

    /**
     * Post a file transfer request.
     *
     * @param request the file transfer request
     * @return the transferring file id
     * @throws Throwable the exception
     */
    @PostMapping("/files/transfer")
    public long transfer(@Valid @RequestBody FileTransferRequest request) throws Throwable {
        return fileService.handleTransferRequest(request);
    }

    /**
     * Query the progress of the file transfer by id.
     *
     * @param transferringFileId the transferring file id
     * @return the file transfer progress
     */
    @GetMapping("/files/transfer/{transferring-file-id}")
    public FileTransferProgress transferProgress(@PathVariable("transferring-file-id") long transferringFileId) {
        return fileService.getTransferProgress(transferringFileId);
    }

    /**
     * Upload a file
     *
     * @param type the file type
     * @param file the file
     * @throws Throwable the exception
     */
    @PostMapping(value = "/files/upload")
    public void upload(@RequestParam FileType type, @RequestParam MultipartFile file) throws Throwable {
        fileService.handleUploadRequest(type, file);
    }

    /**
     * Download a file by id
     *
     * @param fileId   the file id
     * @param response the http response
     * @return Resource
     * @throws Throwable the exception
     */
    @GetMapping("/files/{file-id}/download")
    public ResponseEntity<Resource> download(@PathVariable("file-id") long fileId,
                                             HttpServletResponse response) throws Throwable {
        WrappedResource resource = fileService.handleDownloadRequest(fileId, response);
        if (resource.resource() != null) {
            String contentType = "application/octet-stream";
            return ResponseEntity.ok()
                                 .contentType(MediaType.parseMediaType(contentType))
                                 .header(HttpHeaders.CONTENT_DISPOSITION,
                                         "attachment; filename=\"" + resource.name() + "\"")
                                 .body(resource.resource());
        }
        return null;
    }
}
