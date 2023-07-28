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

public enum JavaThreadState {

    NEW("NEW"),

    RUNNABLE("RUNNABLE"),

    SLEEPING("TIMED_WAITING (sleeping)"),

    IN_OBJECT_WAIT("WAITING (on object monitor)"),

    IN_OBJECT_WAIT_TIMED("TIMED_WAITING (on object monitor)"),

    PARKED("WAITING (parking)"),

    PARKED_TIMED("TIMED_WAITING (parking)"),

    BLOCKED_ON_MONITOR_ENTER("BLOCKED (on object monitor)"),

    TERMINATED("TERMINATED"),

    UNKNOWN("UNKNOWN");

    public static final int COUNT = JavaThreadState.values().length;
    private final String description;

    JavaThreadState(String description) {
        this.description = description;
    }

    public static JavaThreadState getByDescription(String s) {
        if (s == null) {
            throw new IllegalArgumentException();
        }

        for (JavaThreadState state : JavaThreadState.values()) {
            if (s.startsWith(state.description)) {
                return state;
            }
        }
        return UNKNOWN;
    }
}
