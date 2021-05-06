/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

/**
 * @plasma147 provided this solution:
 * https://stackoverflow.com/a/11385215/813561
 * https://creativecommons.org/licenses/by-sa/3.0/
 */

package org.eclipse.jifa.common.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class TypeFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
        Class<? super T> t = type.getRawType();
        if(t.isAnnotationPresent(UseAccessor.class)) { 
            return (TypeAdapter<T>) new AccessorBasedTypeAdaptor(gson);
        } else {
            return null;
        }
    }
}