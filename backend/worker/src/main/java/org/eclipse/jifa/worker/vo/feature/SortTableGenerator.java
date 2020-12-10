/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.worker.vo.feature;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SortTableGenerator {
    private Map<String, Comparator> table;

    public SortTableGenerator() {
        this.table = new HashMap<>();
    }

    public <T> Map<String, Comparator> build() {
        return table;
    }

    public SortTableGenerator add(String key, Comparator comp) {
        table.put(key, comp);
        return this;
    }

    public <T, U extends Comparable<? super U>> SortTableGenerator add(String key,
                                                                       Function<? super T, ? extends U> val) {
        table.put(key, Comparator.comparing(val));
        return this;
    }
}
