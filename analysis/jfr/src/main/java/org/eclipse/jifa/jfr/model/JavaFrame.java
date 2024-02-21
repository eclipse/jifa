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
package org.eclipse.jifa.jfr.model;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.jfr.model.Frame;

public class JavaFrame extends Frame {
    public enum Type {
        INTERPRETER("Interpreted"),
        JIT("JIT compiled"),
        INLINE("Inlined"),
        NATIVE("Native");

        final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type typeOf(String value) {
            for (Type type : Type.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException(value);
        }
    }

    private boolean isJavaFrame;

    @Setter
    @Getter
    private Type type;

    @Setter
    @Getter
    private long bci = -1;

    public boolean isJavaFrame() {
        return isJavaFrame;
    }

    public void setJavaFrame(boolean javaFrame) {
        isJavaFrame = javaFrame;
    }
}
