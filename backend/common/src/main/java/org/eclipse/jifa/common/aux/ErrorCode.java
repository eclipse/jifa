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
package org.eclipse.jifa.common.aux;

public enum ErrorCode {

    ILLEGAL_ARGUMENT,

    FILE_DOES_NOT_EXIST,

    TRANSFER_ERROR,

    SANITY_CHECK,

    UNKNOWN_ERROR,

    SHOULD_NOT_REACH_HERE,
    ;

    public boolean isFatal() {
        switch (this) {
            case ILLEGAL_ARGUMENT:
            case FILE_DOES_NOT_EXIST:
                return false;
            default:
                return true;
        }
    }
}
