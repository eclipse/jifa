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
package org.eclipse.jifa.server.enums;

import org.eclipse.jifa.common.domain.exception.ShouldNotReachHereException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

public enum AnalysisApiResponseCode {

    ACCEPTED,

    SUCCESS,

    FAILURE;

    public int httpStatus(HttpMethod method) {
        if (this == SUCCESS) {
            return HttpStatus.OK.value();
        }

        if (this == FAILURE) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        throw new ShouldNotReachHereException();
    }
}
