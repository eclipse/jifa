/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

import static org.eclipse.jifa.gclog.model.GCEvent.UNKNOWN_DOUBLE;
import static org.eclipse.jifa.gclog.model.GCEvent.UNKNOWN_INT;

public class IntData {

    private int n;
    private long sum;
    private int min = Integer.MAX_VALUE;
    private int max = Integer.MIN_VALUE;

    public void add(int x) {
        sum += x;
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
        return sum;
    }

    public int getMin() {
        if (n == 0) {
            return UNKNOWN_INT;
        }
        return min;
    }

    public int getMax() {
        if (n == 0) {
            return UNKNOWN_INT;
        }
        return max;
    }

    public double average() {
        if (n == 0) {
            return UNKNOWN_DOUBLE;
        }
        return ((double) sum) / ((double) n);
    }
}
