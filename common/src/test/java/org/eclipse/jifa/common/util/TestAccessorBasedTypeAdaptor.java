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
package org.eclipse.jifa.common.util;

import com.google.gson.JsonObject;
import org.eclipse.jifa.common.annotation.UseAccessor;
import org.junit.jupiter.api.Test;

import static org.eclipse.jifa.common.util.GsonHolder.GSON;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAccessorBasedTypeAdaptor {

    @UseAccessor
    static class Person {
        public String getName() {
            return "Mike";
        }

        public boolean isJavaDeveloper() {
            return true;
        }
    }

    @Test
    public void test() {
        Person person = new Person();
        String json = GSON.toJson(new Person());
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
        assertEquals(person.getName(), jsonObject.get("name").getAsString());
        assertEquals(person.isJavaDeveloper(), jsonObject.get("javaDeveloper").getAsBoolean());
    }
}
