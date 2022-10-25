/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.tda.vo;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.eclipse.jifa.tda.enums.MonitorState;

@Data
@AllArgsConstructor
public class VMonitor {

    private int id;

    private long address;

    private boolean classInstance;

    @SerializedName("class")
    private String clazz;

    private MonitorState state;

    public VMonitor(int id, long address, boolean classInstance, String clazz) {
        this.id = id;
        this.address = address;
        this.classInstance = classInstance;
        this.clazz = clazz;
    }
}
