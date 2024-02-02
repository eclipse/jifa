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
package org.eclipse.jifa.profile.exception;

public class ProfileAnalysisException extends Exception {
    public ProfileAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProfileAnalysisException(Throwable cause) {
        super(cause);
    }

    public ProfileAnalysisException(String message) {
        super(message);
    }
}
