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
package org.eclipse.jifa.master.service.impl.helper;

import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.eclipse.jifa.common.ErrorCode;

import static org.eclipse.jifa.master.service.ServiceAssertion.SERVICE_ASSERT;

public class SQLAssert {

    public static void assertSelected(ResultSet resultSet) {
        assertSelected(resultSet, 1);
    }

    public static void assertSelected(ResultSet result, int expected) {
        SERVICE_ASSERT.isTrue(result.getNumRows() == expected, ErrorCode.SANITY_CHECK);
    }

    public static void assertUpdated(UpdateResult result) {
        assertUpdated(result, 1);
    }

    public static void assertUpdated(UpdateResult result, int expected) {
        SERVICE_ASSERT.isTrue(result.getUpdated() == expected, ErrorCode.SANITY_CHECK);
    }
}
