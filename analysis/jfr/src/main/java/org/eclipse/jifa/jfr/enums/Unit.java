/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.jfr.enums;

import org.eclipse.jifa.common.annotation.UseGsonEnumAdaptor;

@UseGsonEnumAdaptor
public enum Unit {
    NANO_SECOND("ns"),

    BYTE("byte"),

    COUNT("count");

    private final String tag;

    Unit(String tag) {
        this.tag = tag;
    }

    public String toString() {
        return tag;
    }
}
