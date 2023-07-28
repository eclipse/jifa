/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.analysis.support;

public class GetterMethodNameConverter implements MethodNameConverter {
    @Override
    public String convert(String name) {
        if (name.startsWith("get") && name.length() > 3) {
            if (Character.isUpperCase(name.charAt(4))) {
                return name.substring(3);
            }
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        return name;
    }
}
