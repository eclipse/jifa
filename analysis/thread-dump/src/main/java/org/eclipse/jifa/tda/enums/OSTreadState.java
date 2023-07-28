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

package org.eclipse.jifa.tda.enums;

public enum OSTreadState {

    ALLOCATED("allocated"),

    INITIALIZED("initialized"),

    RUNNABLE("runnable"),

    MONITOR_WAIT("waiting for monitor entry"),

    COND_VAR_WAIT("waiting on condition"),

    OBJECT_WAIT("in Object.wait()"),

    BREAK_POINTED("at breakpoint"),

    SLEEPING("sleeping"),

    ZOMBIE("zombie"),

    UNKNOWN("unknown state");

    public static final int COUNT = OSTreadState.values().length;
    private final String description;

    OSTreadState(String description) {
        this.description = description;
    }

    public static OSTreadState getByDescription(String s) {
        if (s == null) {
            throw new IllegalArgumentException();
        }

        for (OSTreadState state : OSTreadState.values()) {
            if (s.startsWith(state.description)) {
                return state;
            }
        }
        return UNKNOWN;
    }
}
