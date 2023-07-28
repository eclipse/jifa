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

package org.eclipse.jifa.gclog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_DOUBLE;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TimeRange {
    //unit is ms
    private double start = UNKNOWN_DOUBLE;
    private double end = UNKNOWN_DOUBLE;

    public boolean isValid() {
        return start >= 0 && end >= 0 && start < end;
    }

    public double length() {
        if (isValid()) {
            return end - start;
        } else {
            return UNKNOWN_DOUBLE;
        }
    }

    @Override
    public String toString() {
        return start + " ~ " + end;
    }
}
