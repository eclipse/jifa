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
import org.eclipse.jifa.tda.enums.OSTreadState;
import org.eclipse.jifa.tda.enums.ThreadType;

import java.util.Objects;

@Data
public class Thread extends Identity {

    private String name;

    private int osPriority;

    // ms, optional
    // -1 means unknown
    private double cpu = -1;

    // ms, optional
    // -1 means unknown
    private double elapsed = -1;

    private long tid;

    private long nid;

    private OSTreadState osThreadState;

    private ThreadType type;

    private int lineStart;

    // include
    private int lineEnd;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Thread thread = (Thread) o;
        return osPriority == thread.osPriority && Double.compare(thread.cpu, cpu) == 0 &&
               Double.compare(thread.elapsed, elapsed) == 0 && tid == thread.tid && nid == thread.nid &&
               lineStart == thread.lineStart && lineEnd == thread.lineEnd && Objects.equals(name, thread.name) &&
               osThreadState == thread.osThreadState && type == thread.type;
    }

    @Override
    public int hashCode() {
        return Objects
            .hash(name, osPriority, cpu, elapsed, tid, nid, osThreadState, type, lineStart, lineEnd);
    }
}
