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

import jakarta.validation.Valid;
import org.eclipse.jifa.common.domain.vo.PageView;
import org.eclipse.jifa.server.domain.dto.FileTransferProgress;
import org.eclipse.jifa.server.domain.dto.FileTransferRequest;
import org.eclipse.jifa.server.domain.dto.FileView;
import org.eclipse.jifa.server.domain.dto.NamedResource;
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
    public PageView<FileView> files(@RequestParam(required = false) FileType type, @RequestParam int page, @RequestParam int pageSize) {
        return fileService.getUserFileViews(type, page, pageSize);
    }

    /**
     * Query the file by id.
     *
     * @param key the id or the unique name
     * @return the file view
     */
    @GetMapping("/files/{id-or-unique-name}")
    public FileView file(@PathVariable("id-or-unique-name") String key) {
        try {
            return fileService.getFileViewById(Long.parseLong(key));
        } catch (NumberFormatException e) {
            return fileService.getFileViewByUniqueName(key);
        }
    }
}
