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

import static org.eclipse.jifa.common.enums.CommonErrorCode.VALIDATION_FAILURE;

/**
 * This exception will be thrown when validation failure, see {@link org.eclipse.jifa.common.util.Validate}.
 */
public class ValidationException extends ErrorCodeException {

    /**
     * Create a new ValidationException with a specified error code and a message.
     *
     * @param errorCode error code
     * @param message   message
     */
    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Create a new ValidationException with a specified error code.
     *
     * @param errorCode error code
     */
    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Create a new ValidationException with a message.
     *
     * @param message message
     */
    public ValidationException(String message) {
        super(VALIDATION_FAILURE, message);
    }

    /**
     * Create a new ValidationException.
     */
    public ValidationException() {
        super(VALIDATION_FAILURE);
    }
}
