/********************************************************************************
 * Copyright (c) 2020, 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.server.enums;

import static org.eclipse.jifa.common.domain.exception.CommonException.CE;

public enum FileType {

    HEAP_DUMP("heap-dump"),

    GC_LOG("gc-log"),

    THREAD_DUMP("thread-dump"),

    JFR_FILE("jfr-file");

    private final String storageDirectoryName;

    private final String apiNamespace;

    private final String analysisUrlPath;

    FileType(String tag) {
        storageDirectoryName = tag;
        apiNamespace = tag;
        analysisUrlPath = tag + "-analysis";
    }

    public String getStorageDirectoryName() {
        return storageDirectoryName;
    }

    public String getApiNamespace() {
        return apiNamespace;
    }

    public String getAnalysisUrlPath() {
        return analysisUrlPath;
    }

    public static FileType getByApiNamespace(String expected) {
        for (FileType type : FileType.values()) {
            if (expected.equals(type.apiNamespace)) {
                return type;
            }
        }
        throw CE(ServerErrorCode.UNSUPPORTED_NAMESPACE);
    }
}
