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
package org.eclipse.jifa.gclog;

import org.eclipse.jifa.gclog.util.DoubleData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.eclipse.jifa.gclog.util.Constant.EPS;

public class TestGCLogUtil {
    @Test
    public void testDoubleData() {
        DoubleData doubleData = new DoubleData(true);
        doubleData.add(1);
        doubleData.add(2);
        doubleData.add(3);
        doubleData.add(4);
        Assertions.assertEquals(doubleData.getPercentile(0.99), 0.03 * 3 + 0.97 * 4, EPS);
        Assertions.assertEquals(doubleData.getPercentile(0.75), 0.75 * 3 + 0.25 * 4, EPS);
        doubleData.add(0);
        Assertions.assertEquals(doubleData.getMedian(), 2, EPS);
        Assertions.assertEquals(doubleData.average(), 2, EPS);
        Assertions.assertEquals(doubleData.getMax(), 4, EPS);
        Assertions.assertEquals(doubleData.getMin(), 0, EPS);
        Assertions.assertEquals(doubleData.getN(), 5, EPS);
    }
}
