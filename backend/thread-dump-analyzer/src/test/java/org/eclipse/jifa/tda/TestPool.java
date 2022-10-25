/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.tda;

import org.eclipse.jifa.tda.model.Pool;
import org.junit.Assert;
import org.junit.Test;

public class TestPool extends TestBase {

    @Test
    public void test() {
        Pool<String> sp = new Pool<>();
        sp.add("abc");
        sp.add("ab" + "c");
        sp.add("a" + "bc");
        sp.add("cba");
        Assert.assertEquals(2, sp.size());
        sp.freeze();
    }
}
