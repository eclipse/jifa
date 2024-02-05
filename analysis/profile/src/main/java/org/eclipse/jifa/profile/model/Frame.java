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
package org.eclipse.jifa.profile.model;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.profile.lang.java.model.symbol.SymbolBase;

import java.util.Objects;

public class Frame extends SymbolBase {

    @Setter
    @Getter
    private Method method;

    @Setter
    @Getter
    private int line;

    private String string;

    public String toString() {
        if (this.string != null) {
            return string;
        }

        if (this.line == 0) {
            this.string = method.toString();
        } else {
            this.string = String.format("%s:%d", method, line);
        }

        return this.string;
    }

    public int genHashCode() {
        return Objects.hash(method, line);
    }

    public boolean isEquals(Object b) {
        if (!(b instanceof Frame f2)) {
            return false;
        }

        return line == f2.getLine() && method.equals(f2.getMethod());
    }
}
