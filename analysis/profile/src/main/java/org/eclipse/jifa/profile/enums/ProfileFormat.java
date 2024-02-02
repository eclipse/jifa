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

public enum ProfileFormat {
    JFR("jfr"),
    PPROF("pprof");

    private final String format;

    ProfileFormat(String format) {
        this.format = format;
    }

    public static boolean isJFR(String format) {
        return JFR.format.equalsIgnoreCase(format);
    }

    public static boolean isPPROF(String format) {
        return PPROF.format.equalsIgnoreCase(format);
    }
}
