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
package org.eclipse.jifa.master.service.orm;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import org.eclipse.jifa.master.service.sql.SQL;

import java.time.Instant;

public class SQLHelper {

    public static JsonObject firstRow(ResultSet resultSet) {
        SQLAssert.assertSelected(resultSet);
        return resultSet.getRows().get(0);
    }

    public static int count(ResultSet resultSet) {
        SQLAssert.assertSelected(resultSet);
        return resultSet.getRows().get(0).getInteger(SQL.COUNT_NAME);
    }

    public static JsonArray makeSqlArgument(String param) {
        return new JsonArray().add(param);
    }

    public static JsonArray makeSqlArgument(String param1, String param2) {
        return makeSqlArgument(param1).add(param2);
    }

    public static JsonArray makeSqlArgument(Instant param) {
        return new JsonArray().add(param);
    }

    public static JsonArray makeSqlArgument(Object... params) {
        JsonArray jsonArray = new JsonArray();
        for (Object param : params) {
            if (param instanceof JsonArray) {
                jsonArray.addAll((JsonArray) param);
            } else if (param instanceof Enum) {
                jsonArray.add((Enum) param);
            } else {
                jsonArray.add(param);
            }
        }
        return jsonArray;
    }
}
