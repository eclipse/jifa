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
package org.eclipse.jifa.analysis.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public abstract class TypeParameterUtil {

    // ref: io.netty.util.internal.TypeParameterMatcher
    public static Class<?> extractActualType(Object obj, String typeParamName) {
        Class<?> thisClass = obj.getClass();
        int typeParamIndex = -1;
        TypeVariable<?>[] typeParams = thisClass.getSuperclass().getTypeParameters();
        for (int i = 0; i < typeParams.length; i++) {
            if (typeParamName.equals(typeParams[i].getName())) {
                typeParamIndex = i;
                break;
            }
        }

        if (typeParamIndex < 0) {
            throw new IllegalArgumentException("Cannot match typeParamName: " + typeParamName);
        }

        Type genericSuperType = thisClass.getGenericSuperclass();
        if (!(genericSuperType instanceof ParameterizedType)) {
            throw new RuntimeException();
        }
        Type[] actualTypeParams = ((ParameterizedType) genericSuperType).getActualTypeArguments();

        Type actualTypeParam = actualTypeParams[typeParamIndex];
        if (actualTypeParam instanceof ParameterizedType) {
            actualTypeParam = ((ParameterizedType) actualTypeParam).getRawType();
        }
        if (actualTypeParam instanceof Class) {
            return (Class<?>) actualTypeParam;
        }

        throw new RuntimeException();
    }
}
