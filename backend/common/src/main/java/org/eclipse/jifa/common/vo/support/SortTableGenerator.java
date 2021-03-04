/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.common.vo.support;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SortTableGenerator<T> {

    private final Map<String, Comparator<T>> table;

    public SortTableGenerator() {
        this.table = new HashMap<>();
    }

    public Map<String, Comparator<T>> build() {
        return table;
    }

    public SortTableGenerator<T> add(String key, Comparator<T> comp) {
        table.put(key, comp);
        return this;
    }

    public <U extends Comparable<? super U>> SortTableGenerator<T> add(String key, Function<T, ? extends U> val) {
        table.put(key, Comparator.comparing(val));
        return this;
    }
}
