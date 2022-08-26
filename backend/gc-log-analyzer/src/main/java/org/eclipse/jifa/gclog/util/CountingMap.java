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

import java.util.HashMap;
import java.util.Map;

public class CountingMap<T> {
    private Map<T, Integer> map = new HashMap<>();

    public void put(T key) {
        put(key, 1);
    }

    public void put(T key, int n) {
        map.put(key, map.getOrDefault(key, 0) + n);
    }

    public boolean containKey(T key) {
        return map.containsKey(key);
    }

    public int get(T key) {
        return map.getOrDefault(key, 0);
    }
}
