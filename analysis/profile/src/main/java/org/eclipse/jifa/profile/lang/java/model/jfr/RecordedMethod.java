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

@Setter
@Getter
public class RecordedMethod extends SymbolBase {
    private RecordedClass type;
    private String name;
    private String descriptor;
    private int modifiers;
    private boolean hidden;

    public boolean isEquals(Object b) {
        if (! (b instanceof RecordedMethod)) {
            return false;
        }

        RecordedMethod m2 = (RecordedMethod) b;

        return Objects.equals(descriptor, m2.getDescriptor())
                && Objects.equals(name, m2.getName())
                && modifiers == m2.getModifiers()
                && type.equals(m2.getType())
                && hidden == m2.isHidden();
    }

    public int genHashCode() {
        return Objects.hash(type, name, descriptor, modifiers, hidden);
    }
}
