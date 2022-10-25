/********************************************************************************
 * Copyright (c) 2020, 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.common.enums;

import static org.eclipse.jifa.common.util.ErrorUtil.shouldNotReachHere;

public enum FileType {
    HEAP_DUMP("heap-dump"),

    GC_LOG("gc-log"),

    THREAD_DUMP("thread-dump")
    ;

    private String tag;

    FileType(String tag) {
        this.tag = tag;
    }

    public static FileType getByTag(String tag) {
        for (FileType type : FileType.values()) {
            if (type.tag.equals(tag)) {
                return type;
            }
        }
        return shouldNotReachHere();
    }

    public String getTag() {
        return tag;
    }
}


