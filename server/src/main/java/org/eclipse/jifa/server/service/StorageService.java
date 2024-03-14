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

import org.eclipse.jifa.server.domain.dto.FileTransferRequest;
import org.eclipse.jifa.server.enums.FileType;
import org.eclipse.jifa.server.support.FileTransferListener;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public interface StorageService {

    long getAvailableSpace() throws IOException;

    long getTotalSpace() throws IOException;

    void handleTransfer(FileTransferRequest request, String destFilename, FileTransferListener listener);

    long handleUpload(FileType type, MultipartFile file, String destFilename) throws IOException;

    void handleLocalFile(FileType type, Path path, String destFilename) throws IOException;

    void scavenge(FileType type, String name);

    Path locationOf(FileType type, String name);

    Map<FileType, Set<String>> getAllFiles();

    boolean available();
}
