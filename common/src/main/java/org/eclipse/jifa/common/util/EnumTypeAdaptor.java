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
package org.eclipse.jifa.common.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

@SuppressWarnings({"JavadocDeclaration", "JavadocLinkAsPlainText"})
public class EnumTypeAdaptor<T> extends TypeAdapter<T> {
    public EnumTypeAdaptor() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void write(JsonWriter out, T value) throws IOException {
        out.jsonValue("\"" + value.toString() + "\"");
    }

    @Override
    public T read(JsonReader in) {
        throw new UnsupportedOperationException("Only supports writes.");
    }
}