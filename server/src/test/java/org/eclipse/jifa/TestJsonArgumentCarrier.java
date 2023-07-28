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
package org.eclipse.jifa;

import org.junit.jupiter.api.Test;

public class TestJsonArgumentCarrier {

    enum E {
        A, B
    }

    static class Person {
        String name;
        int age;
    }

    @Test
    public void test() {
//        JsonArgumentCarrier argumentCarrier = new JsonArgumentCarrier(GSON.fromJson(
//                """
//                        {
//                          "b": true,
//                          "i": 1,
//                          "l": 1,
//                          "d": 1.0,
//                          "s": "string",
//                          "e": "B",
//                          "p": {"name": "J", "age": 20}
//                        }
//                        """,
//                JsonObject.class));
//
//        assertTrue(argumentCarrier.getBoolean("b"));
//        assertEquals(1, argumentCarrier.getInt("i"));
//        assertEquals(1L, argumentCarrier.getLong("l"));
//        assertEquals(1.0, argumentCarrier.getDouble("d"));
//        assertEquals("string", argumentCarrier.getString("s"));
//        assertEquals(E.B, argumentCarrier.getEnum("e", E.class));
//        Person p = argumentCarrier.getObject("p", Person.class);
//        assertEquals("J", p.name);
//        assertEquals(20, p.age);
    }
}
