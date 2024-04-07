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
package org.eclipse.jifa.analysis.enums;

import org.eclipse.jifa.common.domain.exception.ErrorCode;

public enum AnalysisErrorCode implements ErrorCode {

    FILE_NOT_FOUND("File not found"),
    ;

    private final String message;

    AnalysisErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
