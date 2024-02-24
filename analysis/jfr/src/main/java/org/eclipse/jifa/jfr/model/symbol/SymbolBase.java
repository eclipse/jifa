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
package org.eclipse.jifa.jfr.model.symbol;

public abstract class SymbolBase {
    private Integer hashCode = null;

    public abstract int genHashCode();

    public abstract boolean isEquals(Object b);

    public boolean equals(Object b) {
        if (this == b) {
            return true;
        }

        if (b == null) {
            return false;
        }

        if (!(b instanceof SymbolBase)) {
            return false;
        }

        return isEquals(b);
    }

    public int hashCode() {
        if (hashCode == null) {
            hashCode = genHashCode();
        }

        return hashCode;
    }
}