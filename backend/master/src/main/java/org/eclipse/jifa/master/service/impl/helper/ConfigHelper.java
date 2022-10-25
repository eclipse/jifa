/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.master.service.impl.helper;

import io.vertx.core.json.JsonObject;

public class ConfigHelper {

    public static int getInt(JsonObject json) {
        return Integer.parseInt(json.getString("value"));
    }

    public static long getLong(JsonObject json) {
        return Long.parseLong(json.getString("value"));
    }

    public static boolean getBoolean(JsonObject json) {
        return Boolean.parseBoolean(json.getString("value"));
    }

    public static String getString(JsonObject json) {
        return json.getString("value");
    }
}
