/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.profile.enums;

public enum FileFormat {
    JFR(new String[] {".jfr"}),
    PPROF(new String[] {".pprof", ".profile"}),
    COLLAPSED(new String[] {".collapsed"}),
    TEXT(new String[] {".txt"});

    FileFormat(String[] patterns) {
        this.patterns = patterns;
    }

    private final String[] patterns;
    private static final FileFormat[] FORMATS = new FileFormat[] { JFR, PPROF, COLLAPSED, TEXT };
    public static FileFormat of(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        for (FileFormat format : FORMATS) {
            for (String pattern : format.patterns) {
                if (fileName.toLowerCase().endsWith(pattern)) {
                    return format;
                }
            }
        }
        return null;
    }

    public static boolean isJFR(String fileName) {
        return of(fileName) == JFR;
    }

    public static boolean isPPROF(String fileName) {
        return of(fileName) == PPROF;
    }

    public static boolean isCollapsed(String fileName) {
        return of(fileName) == COLLAPSED;
    }

    public static boolean isText(String fileName) {
        return of(fileName) == TEXT;
    }
}
