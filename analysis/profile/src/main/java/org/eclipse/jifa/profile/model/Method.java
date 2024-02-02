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

@Setter
@Getter
public class Method extends SymbolBase {
    private String packageName;

    private String type;

    private String name;

    private String descriptor;

    private String string;

    public int genHashCode() {
        return Objects.hash(packageName, type, name, descriptor);
    }

    public boolean isEquals(Object b) {
        if (! (b instanceof Method)) {
            return false;
        }

        Method m2 = (Method) b;

        return Objects.equals(packageName, m2.getPackageName())
                && Objects.equals(type, m2.getType())
                && Objects.equals(name, m2.getName())
                && Objects.equals(descriptor, m2.getDescriptor());
    }

    public String toString(boolean includeDescriptor) {
        if (string != null) {
            return string;
        }

        String str;
        if (this.descriptor != null && includeDescriptor) {
            str = String.format("%s%s", this.name, this.descriptor);
        } else {
            str = this.name;
        }

        if (this.type != null) {
            str = String.format("%s.%s", this.type, str);
        }

        if (this.packageName != null) {
            str = String.format("%s.%s", this.packageName, str);
        }

        this.string = str;

        return str;
    }

    public String toString() {
        return toString(true);
    }
}
