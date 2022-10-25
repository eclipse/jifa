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
package org.eclipse.jifa.master.model;

import static org.eclipse.jifa.master.Constant.*;

public enum TransferWay {

    URL(uri(TRANSFER_BY_URL), "url"),

    SCP(uri(TRANSFER_BY_SCP), "path"),

    OSS(uri(TRANSFER_BY_OSS), "objectName"),

    S3(uri(TRANSFER_BY_S3), "objectName"),

    UPLOAD(uri(FILE_UPLOAD), "");

    private String[] pathKey;

    private String uri;

    TransferWay(String uri, String... pathKey) {
        this.pathKey = pathKey;
        this.uri = uri;
    }

    public String[] getPathKeys() {
        return pathKey;
    }

    public String getUri() {
        return uri;
    }
}
