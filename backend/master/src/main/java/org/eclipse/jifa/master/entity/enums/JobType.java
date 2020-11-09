/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.master.entity.enums;

import org.eclipse.jifa.common.enums.FileType;

public enum JobType {

    FILE_TRANSFER,

    HEAP_DUMP_ANALYSIS,

    METASPACE_DUMP_ANALYSIS,

    JINSIGHT,

    PORTRAIT;

    public boolean isFileTransfer() {
        return this == FILE_TRANSFER;
    }

    public String getTag() {
        switch (this) {
            case HEAP_DUMP_ANALYSIS:
                return FileType.HEAP_DUMP.getTag();
            default:
                throw new IllegalStateException();
        }
    }
}
