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
package org.eclipse.jifa.common;

public class JifaException extends RuntimeException {

    private ErrorCode code = ErrorCode.UNKNOWN_ERROR;

    public JifaException() {
        this(ErrorCode.UNKNOWN_ERROR);
    }

    public JifaException(String detail) {
        this(ErrorCode.UNKNOWN_ERROR, detail);
    }

    public JifaException(ErrorCode code) {
        this(code, code.name());
    }

    public JifaException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    public JifaException(Throwable cause) {
        super(cause);
    }

    public JifaException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.code = errorCode;
    }

    public ErrorCode getCode() {
        return code;
    }
}
