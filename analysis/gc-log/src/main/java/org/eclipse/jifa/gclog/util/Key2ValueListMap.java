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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Key2ValueListMap<K, V> {
    private Map<K, List<V>> map;

    public Key2ValueListMap(Map<K, List<V>> map) {
        this.map = map;
    }

    public Key2ValueListMap() {
        map = new HashMap<>();
    }

    public void put(K key, V value) {
        List<V> list = map.getOrDefault(key, null);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(value);
    }

    public List<V> get(K key) {
        return map.getOrDefault(key, null);
    }

    public Map<K, List<V>> getInnerMap() {
        return map;
    }
}
