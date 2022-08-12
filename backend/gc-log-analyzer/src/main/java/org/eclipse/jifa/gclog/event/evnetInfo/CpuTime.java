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

package org.eclipse.jifa.gclog.event.evnetInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_DOUBLE;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CpuTime {
    //unit is ms
    private double user = UNKNOWN_DOUBLE;
    private double sys = UNKNOWN_DOUBLE;
    private double real = UNKNOWN_DOUBLE;

    public String toString() {
        return String.format("User=%.2fs Sys=%.2fs Real=%.2fs", user / 1000, sys / 1000, real / 1000);
    }

}
