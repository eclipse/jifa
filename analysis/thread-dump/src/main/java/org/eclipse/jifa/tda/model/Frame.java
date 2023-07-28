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
import org.eclipse.jifa.tda.enums.SourceType;

import java.util.Arrays;
import java.util.Objects;

@Data
public class Frame {

    private String clazz;

    private String method;

    private String module;

    private SourceType sourceType;

    // -1 means unknown
    private String source;

    // -1 means unknown
    private int line = -1;

    private Monitor[] monitors;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Frame frame = (Frame) o;
        return line == frame.line && Objects.equals(clazz, frame.clazz) &&
               Objects.equals(method, frame.method) && Objects.equals(module, frame.module) &&
               sourceType == frame.sourceType && Objects.equals(source, frame.source) &&
               Arrays.equals(monitors, frame.monitors);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(clazz, method, module, sourceType, source, line);
        result = 31 * result + Arrays.hashCode(monitors);
        return result;
    }
}
