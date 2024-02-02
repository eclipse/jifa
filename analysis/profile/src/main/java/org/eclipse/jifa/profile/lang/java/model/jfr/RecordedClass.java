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
package org.eclipse.jifa.profile.lang.java.model.jfr;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.profile.lang.java.model.symbol.SymbolBase;

import java.util.Objects;

public class RecordedClass extends SymbolBase {
    @Setter
    @Getter
    private String packageName;
    @Setter
    @Getter
    private String name;
    private String fullName;

    public String getFullName() {
        if (fullName == null) {
            fullName = packageName + "." + name;
        }
        return fullName;
    }

    public boolean isEquals(Object b) {
        if (!(b instanceof RecordedClass)) {
            return false;
        }

        RecordedClass c2 = (RecordedClass) b;

        return Objects.equals(packageName, c2.getPackageName()) && Objects.equals(name, c2.getName());
    }

    public int genHashCode() {
        return Objects.hash(packageName, name);
    }
}
