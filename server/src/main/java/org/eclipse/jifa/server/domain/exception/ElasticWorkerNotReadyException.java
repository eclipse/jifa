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
package org.eclipse.jifa.server.domain.exception;

import org.eclipse.jifa.common.domain.exception.ErrorCodeAccessor;
import org.eclipse.jifa.server.enums.ServerErrorCode;

public class ElasticWorkerNotReadyException extends RuntimeException implements ErrorCodeAccessor {

    private final long id;

    public ElasticWorkerNotReadyException(long id) {
        this.id = id;
    }

    @Override
    public ServerErrorCode getErrorCode() {
        return ServerErrorCode.ELASTIC_WORKER_NOT_READY;
    }
}
