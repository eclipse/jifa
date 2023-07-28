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
package org.eclipse.jifa.analysis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

public class TestExecutionContext {

    @SuppressWarnings("DuplicateExpressions")
    @Test
    public void testEquality() {
        ExecutionContext context1 = new ExecutionContext(Paths.get("a/b/c"), "api", new Object[]{1, 2.0, "3", true});
        ExecutionContext context2 = new ExecutionContext(Paths.get("a/b/c"), "api", new Object[]{1, 2.0, "3", true});
        Assertions.assertEquals(context1, context2);
        Assertions.assertEquals(context1.hashCode(), context2.hashCode());
    }
}
