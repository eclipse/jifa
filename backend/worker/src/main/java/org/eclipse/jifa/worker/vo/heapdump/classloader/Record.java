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
package org.eclipse.jifa.worker.vo.heapdump.classloader;

import lombok.Data;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.worker.vo.feature.SearchType;
import org.eclipse.jifa.worker.vo.feature.Searchable;
import org.eclipse.jifa.worker.vo.feature.SortTableGenerator;

import java.util.Comparator;
import java.util.Map;

@Data
public class Record implements Searchable {

    private static Map<String, Comparator> sortTable = new SortTableGenerator()
            .add("id", Record::getObjectId)
            .add("numberOfInstances", Record::getNumberOfInstances)
            .add("definedClasses", Record::getDefinedClasses)
            .build();
    private int objectId;

    private String prefix;

    private String label;

    private boolean classLoader;

    private boolean hasParent;

    private int definedClasses;

    private int numberOfInstances;

    public static Comparator<Record> sortBy(String field, boolean ascendingOrder) {
        return ascendingOrder ? sortTable.get(field) : sortTable.get(field).reversed();
    }

    @Override
    public Object getBySearchType(SearchType type) {
        ErrorUtil.unimplemented();
        return null;
    }
}
