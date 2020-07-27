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
package org.eclipse.jifa.worker.vo.heapdump.overview;

import lombok.Data;

@Data
public class BigObject {

    private String label;

    private int objectId;

    private double value;

    private String description;

    public BigObject(String label, int objectId, double value, String description) {
        this.label = label;
        this.objectId = objectId;
        this.value = value;
        this.description = description;
    }
}
