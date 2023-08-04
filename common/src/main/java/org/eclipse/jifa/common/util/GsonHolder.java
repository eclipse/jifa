/********************************************************************************
 * Copyright (c) 2020, 2023 Contributors to the Eclipse Foundation
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import org.eclipse.jifa.common.annotation.UseAccessor;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gson instance Holder
 */
public interface GsonHolder {

    /**
     * The gson instance used by all jifa java code
     */
    Gson GSON = new GsonBuilder().registerTypeAdapterFactory(new TypeFactory())
                                 .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                                 .serializeSpecialFloatingPointValues()
                                 .create();

    /**
     * Custom type factory
     */
    class TypeFactory implements TypeAdapterFactory {

        /**
         * @return a AccessorBasedTypeAdaptor if the type is annotated with {@link UseAccessor}, null otherwise.
         */
        public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
            Class<? super T> t = type.getRawType();
            if (t.isAnnotationPresent(UseAccessor.class)) {
                return new AccessorBasedTypeAdaptor<>(gson);
            } else {
                return null;
            }
        }
    }

    class LocalDateTimeTypeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(FORMATTER.format(src));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), FORMATTER);
        }

    }
}
