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

import java.util.Arrays;
import java.util.Objects;

@Setter
@Getter
public class StackTrace extends SymbolBase {

    private Frame[] frames;

    private boolean truncated;

    public int genHashCode() {
        return Objects.hash(truncated, Arrays.hashCode(frames));
    }

    public boolean isEquals(Object b) {
        if (!(b instanceof StackTrace t2)) {
            return false;
        }

        return truncated == t2.truncated && Arrays.equals(frames, t2.frames);
    }
}
