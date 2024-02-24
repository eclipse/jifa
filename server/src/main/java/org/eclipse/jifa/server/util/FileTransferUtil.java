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
package org.eclipse.jifa.server.util;

import org.eclipse.jifa.common.domain.exception.ShouldNotReachHereException;
import org.eclipse.jifa.server.domain.dto.FileTransferRequest;
import org.eclipse.jifa.server.enums.FileTransferMethod;

public abstract class FileTransferUtil {

    public static String extractOriginalName(FileTransferRequest request) {
        FileTransferMethod method = request.getMethod();
        String source = switch (method) {
            case OSS -> request.getOssObjectKey();
            case S3 -> request.getS3ObjectKey();
            case SCP -> request.getScpSourcePath();
            case URL -> request.getUrl();
            case TEXT -> request.getFilename();
            case UPLOAD -> throw new ShouldNotReachHereException();
        };

        String name = source.substring(source.lastIndexOf(java.io.File.separatorChar) + 1);
        if (method == FileTransferMethod.URL && name.contains("?")) {
            name = name.substring(0, name.indexOf("?"));
        }

        return name;
    }
}
