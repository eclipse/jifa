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
package org.eclipse.jifa.common.util;

import org.eclipse.jifa.common.ErrorCode;
import org.eclipse.jifa.common.JifaException;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorUtil {

    public static String toString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static void throwEx(Exception e) {
        throw new JifaException(e);
    }

    public static <T> T shouldNotReachHere() {
        throw new JifaException(ErrorCode.SHOULD_NOT_REACH_HERE);
    }

    public static void errorWith(ErrorCode code, String detail) {
        throw new JifaException(code, detail);
    }
}
