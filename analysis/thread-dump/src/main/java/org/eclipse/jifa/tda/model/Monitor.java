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

package org.eclipse.jifa.tda.model;

import lombok.Data;
import org.eclipse.jifa.tda.enums.MonitorState;

import java.util.Objects;

@Data
public class Monitor {

    private RawMonitor rawMonitor;

    private MonitorState state;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Monitor monitor = (Monitor) o;
        return Objects.equals(rawMonitor, monitor.rawMonitor) && state == monitor.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawMonitor, state);
    }
}
