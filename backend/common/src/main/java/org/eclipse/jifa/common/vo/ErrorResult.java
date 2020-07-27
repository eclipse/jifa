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
package org.eclipse.jifa.common.vo;

import lombok.Data;
import org.eclipse.jifa.common.aux.ErrorCode;
import org.eclipse.jifa.common.aux.JifaException;

@Data
public class ErrorResult {

    private ErrorCode errorCode = ErrorCode.UNKNOWN_ERROR;

    private String message;

    public ErrorResult(Throwable t) {
        if (t instanceof JifaException) {
            errorCode = ((JifaException) t).getCode();
        }

        if (t instanceof IllegalArgumentException) {
            errorCode = ErrorCode.ILLEGAL_ARGUMENT;
        }

        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof JifaException) {
            message = cause.getMessage();
        } else {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
    }
}
