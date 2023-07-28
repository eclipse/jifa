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

import static org.eclipse.jifa.common.enums.CommonErrorCode.INTERNAL_ERROR;

/**
 * Common Exception.
 * Use this exception when you don't know what exception to use.
 */
public class CommonException extends ErrorCodeException {

    /**
     * Create a new CommonException.
     *
     * @param errorCode error code
     * @param message   message
     */
    public CommonException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Create a new CommonException.
     *
     * @param errorCode error code
     */
    public CommonException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Create a new CommonException.
     *
     * @param message message
     */
    public CommonException(String message) {
        super(INTERNAL_ERROR, message);
    }

    /**
     * Create a new CommonException.
     *
     * @param throwable cause
     */
    public CommonException(Throwable throwable) {
        super(INTERNAL_ERROR, throwable);
    }

    /**
     * a shortcut for new CommonException(errorCode)
     *
     * @param errorCode error code
     * @return new common exception
     */
    public static CommonException CE(ErrorCode errorCode) {
        return new CommonException(errorCode);
    }
}
