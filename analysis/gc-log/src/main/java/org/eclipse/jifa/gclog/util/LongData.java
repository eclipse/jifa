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

package org.eclipse.jifa.gclog.util;

import java.math.BigInteger;

import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_DOUBLE;
import static org.eclipse.jifa.gclog.util.Constant.UNKNOWN_INT;

public class LongData {

    private int n;
    private BigInteger sum = BigInteger.ZERO;
    private long min = Integer.MAX_VALUE;
    private long max = Integer.MIN_VALUE;

    public void add(long x) {
        if (x == UNKNOWN_INT) {
            return;
        }
        sum = sum.add(BigInteger.valueOf(x));
        n++;
        min = Math.min(min, x);
        max = Math.max(max, x);
    }

    public int getN() {
        return n;
    }

    public long getSum() {
        if (n == 0) {
            return UNKNOWN_INT;
        }
        return sum.longValue();
    }

    public long getMin() {
        if (n == 0) {
            return UNKNOWN_INT;
        }
        return min;
    }

    public long getMax() {
        if (n == 0) {
            return UNKNOWN_INT;
        }
        return max;
    }

    public double average() {
        if (n == 0) {
            return UNKNOWN_DOUBLE;
        }
        return sum.divide(BigInteger.valueOf(n)).doubleValue();
    }
}
