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

package org.eclipse.jifa.tda.util;

public class Converter {

    public static double str2TimeMillis(String str) {
        if (str == null) {
            return -1;
        }
        int length = str.length();
        if (str.endsWith("ms")) {
            return Double.parseDouble(str.substring(0, length - 2));
        } else if (str.endsWith("s")) {
            return Double.parseDouble(str.substring(0, length - 1)) * 1000;
        }
        throw new IllegalArgumentException(str);
    }
}
