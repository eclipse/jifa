/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtil {

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValueOrNull(Object node, String fieldName) {
        try {
            Field field = node.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(node);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return null;
    }
}