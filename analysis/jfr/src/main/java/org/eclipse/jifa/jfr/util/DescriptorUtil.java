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
package org.eclipse.jifa.jfr.util;

import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class DescriptorUtil {
    private final Map<String, String> CACHE = new HashMap<>();

    public String decodeMethodArgs(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return "";
        }

        if (CACHE.containsKey(descriptor)) {
            return CACHE.get(descriptor);
        }

        Type methodType = Type.getMethodType(descriptor);
        StringBuilder b = new StringBuilder("(");
        Type[] argTypes = methodType.getArgumentTypes();
        for (int ix = 0; ix < argTypes.length; ix++) {
            if (ix != 0) {
                b.append(", ");
            }
            b.append(trimPackage(argTypes[ix].getClassName()));
        }
        b.append(')');
        String str = b.toString();
        CACHE.put(descriptor, str);

        return str;
    }

    private static String trimPackage(String className) {
        return className.contains(".") ? className.substring(className.lastIndexOf(".") + 1) : className;
    }
}