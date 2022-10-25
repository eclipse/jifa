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
package org.eclipse.jifa.master.vo;

import lombok.Data;

@Data
public class TransferDestination {

    private String hostIP;

    private String directory;

    private String filename;

    public TransferDestination(String hostIP, String directory, String filename) {
        this.hostIP = hostIP;
        this.directory = directory;
        this.filename = filename;
    }
}
