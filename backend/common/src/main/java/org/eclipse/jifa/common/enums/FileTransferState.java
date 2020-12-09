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
package org.eclipse.jifa.common.enums;

public enum FileTransferState {

    NOT_STARTED,

    IN_PROGRESS,

    SUCCESS,

    ERROR;

    public static FileTransferState fromProgressState(ProgressState progress) {
        switch (progress) {
            case NOT_STARTED:
                return NOT_STARTED;
            case IN_PROGRESS:
                return IN_PROGRESS;
            case SUCCESS:
                return SUCCESS;
            case ERROR:
                return ERROR;
        }
        throw new IllegalStateException();
    }

    public boolean isFinal() {
        return this == SUCCESS || this == ERROR;
    }

    public ProgressState toProgressState() {
        switch (this) {
            case NOT_STARTED:
                return ProgressState.NOT_STARTED;

            case IN_PROGRESS:
                return ProgressState.IN_PROGRESS;

            case SUCCESS:
                return ProgressState.SUCCESS;

            case ERROR:
                return ProgressState.ERROR;
        }
        throw new IllegalStateException();
    }
}
