/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.common.domain.exception;

/**
 * The super class of all exceptions that have an {@link ErrorCode}.
 */
public class ErrorCodeException extends RuntimeException implements ErrorCodeAccessor {

    /**
     * error code
     */
    private final ErrorCode errorCode;

    /**
     * Create a new ErrorCodeException with a specified error code
     *
     * @param errorCode error code
     */
    public ErrorCodeException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /**
     * Create a new ErrorCodeException with a specified error code and a message
     *
     * @param errorCode error code
     * @param message   message
     */
    public ErrorCodeException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Create a new ErrorCodeException with a specified error code and a cause
     *
     * @param errorCode error code
     * @param cause     message
     */
    public ErrorCodeException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    /**
     * @return the error code
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
