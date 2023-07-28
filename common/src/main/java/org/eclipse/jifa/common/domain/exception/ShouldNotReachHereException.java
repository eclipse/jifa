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
 * This exception means that the code should not reach here.
 */
public class ShouldNotReachHereException extends ErrorCodeException {

    /**
     * Create a new ShouldNotReachHereException.
     */
    public ShouldNotReachHereException() {
        super(INTERNAL_ERROR);
    }
}
