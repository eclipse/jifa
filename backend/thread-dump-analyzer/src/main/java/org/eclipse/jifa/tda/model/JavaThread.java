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
import org.eclipse.jifa.tda.enums.JavaThreadState;

import java.util.Objects;

@Data
public class JavaThread extends Thread {

    private long jid;

    private boolean daemon;

    private int priority;

    private long lastJavaSP;

    private JavaThreadState javaThreadState;

    private Trace trace;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        JavaThread that = (JavaThread) o;
        return jid == that.jid && daemon == that.daemon && priority == that.priority && lastJavaSP == that.lastJavaSP &&
               javaThreadState == that.javaThreadState && Objects.equals(trace, that.trace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), jid, daemon, priority, lastJavaSP, javaThreadState, trace);
    }
}
