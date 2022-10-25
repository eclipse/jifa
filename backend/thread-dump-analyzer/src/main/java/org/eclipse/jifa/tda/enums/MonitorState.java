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

public enum MonitorState {
    WAITING_ON("- waiting on"),

    WAITING_TO_RE_LOCK("- waiting to re-lock"),

    WAITING_ON_NO_OBJECT_REFERENCE_AVAILABLE("- waiting on"),

    PARKING("- parking"),

    WAITING_ON_CLASS_INITIALIZATION("- waiting on the Class initialization monitor"),

    LOCKED("- locked"),

    WAITING_TO_LOCK("- waiting to lock"),

    ELIMINATED_SCALAR_REPLACED("- eliminated <owner is scalar replaced>"),

    ELIMINATED("- eliminated");

    private final String prefix;

    MonitorState(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}

