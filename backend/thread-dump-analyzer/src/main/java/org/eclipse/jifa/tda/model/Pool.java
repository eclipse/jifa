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

package org.eclipse.jifa.tda.model;

import lombok.Data;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Pool<O> {

    Map<O, O> map;

    public Pool() {
        map = new ConcurrentHashMap<>();
    }

    public O add(O o) {
        return map.computeIfAbsent(o, k -> k);
    }

    public int size() {
        return map.size();
    }

    public void freeze() {
        map = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Pool<?> pool = (Pool<?>) o;
        return Objects.equals(map, pool.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }
}