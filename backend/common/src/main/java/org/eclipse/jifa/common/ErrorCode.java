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

public enum ErrorCode {

    SHOULD_NOT_REACH_HERE,

    UNKNOWN_ERROR,

    ILLEGAL_ARGUMENT,

    SANITY_CHECK,

    FILE_DOES_NOT_EXIST,

    FILE_HAS_BEEN_DELETED,

    TRANSFER_ERROR,

    NOT_TRANSFERRED,

    FILE_TYPE_MISMATCHED,

    HOST_IP_MISMATCHED,

    TRANSFERRING,

    UPLOADING,

    UPLOAD_TO_OSS_ERROR,

    /**
     * for master
     */
    DUMMY_ERROR_CODE,

    FORBIDDEN,

    PENDING_JOB,

    IMMEDIATE_JOB,

    JOB_DOES_NOT_EXIST,

    WORKER_DOES_NOT_EXIST,

    WORKER_DISABLED,

    PRIVATE_HOST_IP,

    REPEATED_USER_WORKER,

    SERVER_TOO_BUSY,

    UNSUPPORTED_OPERATION,

    FILE_IS_IN_USED,

    FILE_IS_BEING_DELETING,

    RETRY,

    RELEASE_PENDING_JOB,

    READINESS_PROBE_FAILURE;

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
