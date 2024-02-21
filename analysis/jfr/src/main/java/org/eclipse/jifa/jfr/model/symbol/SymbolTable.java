/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.jfr.model.symbol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SymbolTable<T> {
    private final Map<T, T> table = new ConcurrentHashMap<>();

    public boolean isContains(T s) {
        return table.containsKey(s);
    }

    public T get(T s) {
        return table.get(s);
    }

    public T put(T s) {
        return table.put(s, s);
    }

    public void clear() {
        this.table.clear();
    }
}
