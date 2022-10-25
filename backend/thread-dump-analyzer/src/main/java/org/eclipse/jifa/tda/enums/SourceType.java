/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.tda.enums;

public enum SourceType {
    REDEFINED,

    NATIVE_METHOD,

    SOURCE_FILE,

    SOURCE_FILE_WITH_LINE_NUMBER,

    UNKNOWN_SOURCE;

    public static SourceType judge(String source) {
        if (source.contains(":")) {
            return SOURCE_FILE_WITH_LINE_NUMBER;
        }

        if (source.endsWith(".java")) {
            return SOURCE_FILE;
        }

        if (source.equals("Redefined")) {
            return REDEFINED;
        }

        if (source.equals("Native Method")) {
            return NATIVE_METHOD;
        }

        if (source.equals("Unknown Source")) {
            return UNKNOWN_SOURCE;
        }
        return SOURCE_FILE;
    }
}
