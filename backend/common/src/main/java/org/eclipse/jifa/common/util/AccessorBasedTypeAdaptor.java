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

import java.io.IOException;
import java.lang.reflect.Method;

import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.eclipse.jifa.common.JifaException;

public class AccessorBasedTypeAdaptor<T> extends TypeAdapter<T> {
    private Gson gson;

    public AccessorBasedTypeAdaptor(Gson gson) {
        this.gson = gson;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(JsonWriter out, T value) throws IOException {
        out.beginObject();
        for (Method method : value.getClass().getMethods()) {
            boolean nonBooleanAccessor = method.getName().startsWith("get");
            boolean booleanAccessor = method.getName().startsWith("is");
            if ((nonBooleanAccessor || booleanAccessor) && !method.getName().equals("getClass") && method.getParameterTypes().length == 0) {
                try {
                    String name = method.getName().substring(nonBooleanAccessor ? 3 : 2);
                    name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
                    Object returnValue = method.invoke(value);
                    if(returnValue != null) {
                        TypeToken<?> token = TypeToken.get(returnValue.getClass());
                        TypeAdapter adapter = gson.getAdapter(token);
                        out.name(name);
                        adapter.write(out, returnValue);
                    }
                } catch (Exception e) {
                    throw new JifaException(e);
                }
            }
        }
        out.endObject();
    }

    @Override
    public T read(JsonReader in) throws IOException {
        throw new UnsupportedOperationException("Only supports writes.");
    }
}