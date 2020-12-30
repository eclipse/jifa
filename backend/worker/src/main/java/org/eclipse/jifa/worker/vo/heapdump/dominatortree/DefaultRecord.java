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
package org.eclipse.jifa.worker.vo.heapdump.dominatortree;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.worker.vo.feature.SearchType;
import org.eclipse.jifa.worker.vo.feature.Searchable;
import org.eclipse.jifa.worker.vo.feature.SortTableGenerator;

import java.util.Comparator;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class DefaultRecord extends BaseRecord implements Searchable {
    private static Map<String, Comparator> sortTable = new SortTableGenerator()
            .add("id", DefaultRecord::getObjectId)
            .add("shallowHeap", DefaultRecord::getShallowSize)
            .add("retainedHeap", DefaultRecord::getRetainedSize)
            .add("percent", DefaultRecord::getPercent)
            .build();

    public static Comparator<DefaultRecord> sortBy(String field, boolean ascendingOrder) {
        return ascendingOrder ? sortTable.get(field) : sortTable.get(field).reversed();
    }

    @Override
    public Object getBySearchType(SearchType type) {
        switch (type) {
            case BY_NAME:
                return getLabel();
            case BY_PERCENT:
                return getPercent();
            case BY_OBJ_NUM:
                return null;
            case BY_RETAINED_SIZE:
                return getRetainedSize();
            case BY_SHALLOW_SIZE:
                return getShallowSize();
            default:
                ErrorUtil.shouldNotReachHere();
        }
        return null;
    }
}
