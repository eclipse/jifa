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
package org.eclipse.jifa.server.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.server.enums.FileTransferMethod;
import org.eclipse.jifa.common.domain.exception.ShouldNotReachHereException;
import org.eclipse.jifa.server.enums.FileType;

@Getter
@Setter
public class FileTransferRequest {

    @NotNull
    private FileTransferMethod method;

    @NotNull
    private FileType type;

    private OSS oss;

    private S3 s3;

    private SCP scp;

    private String url;

    @Getter
    @Setter
    public static class OSS {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
        private String objectName;
    }

    @Getter
    @Setter
    public static class S3 {
        private String region;
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String objectName;
    }

    @Getter
    @Setter
    public static class SCP {
        private String hostname;
        private String user;
        private String password;
        private String path;
    }

    public String extractOriginalName() {
        String source = switch (method) {
            case OSS -> oss.getObjectName();
            case S3 -> s3.getObjectName();
            case SCP -> scp.getPath();
            case URL -> url;
            default -> throw new ShouldNotReachHereException();
        };

        String name = source.substring(source.lastIndexOf(java.io.File.separatorChar) + 1);
        if (name.contains("?")) {
            name = name.substring(0, name.indexOf("?"));
        }
        return name.replaceAll("[%\\\\& ]", "_");
    }
}
