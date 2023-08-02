package org.eclipse.jifa.server.util;

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
        };

        String name = source.substring(source.lastIndexOf(java.io.File.separatorChar) + 1);
        if (method == FileTransferMethod.URL && name.contains("?")) {
            name = name.substring(0, name.indexOf("?"));
        }

        return name;
    }
}
