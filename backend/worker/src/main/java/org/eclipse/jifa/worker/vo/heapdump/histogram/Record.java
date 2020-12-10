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
package org.eclipse.jifa.worker.vo.heapdump.histogram;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.worker.vo.feature.SearchType;
import org.eclipse.jifa.worker.vo.feature.Searchable;
import org.eclipse.jifa.worker.vo.feature.SortTableGenerator;

import java.util.Comparator;
import java.util.Map;

@Data
@NoArgsConstructor
public class Record implements Searchable {
    private long numberOfObjects;
    private long shallowSize;
    private long numberOfYoungObjects;
    private long shallowSizeOfYoung;
    private long numberOfOldObjects;
    private long shallowSizeOfOld;
    private long retainedSize;
    private String label;
    private int objectId;

    private int type;

    public Record(int objectId, String label, int type, long numberOfObjects, long shallowSize, long retainedSize,
                  long numberOfYoungObjects, long shallowSizeOfYoung, long numberOfOldObjects, long shallowSizeOfOld) {
        this.objectId = objectId;
        this.label = label;
        this.type = type;
        this.numberOfObjects = numberOfObjects;
        this.shallowSize = shallowSize;
        this.retainedSize = retainedSize;

        this.numberOfYoungObjects = numberOfYoungObjects;
        this.shallowSizeOfYoung = shallowSizeOfYoung;
        this.numberOfOldObjects = numberOfOldObjects;
        this.shallowSizeOfOld = shallowSizeOfOld;
    }

    private static Map<String, Comparator> sortTable = new SortTableGenerator()
            .add("id", Record::getObjectId)
            .add("numberOfObjects", Record::getNumberOfObjects)
            .add("shallowSize", Record::getShallowSize)
            .add("numberOfYoungObjects", Record::getNumberOfYoungObjects)
            .add("shallowSizeOfYoung", Record::getShallowSizeOfYoung)
            .add("numberOfOldObjects", Record::getNumberOfOldObjects)
            .add("shallowSizeOfOld", Record::getShallowSizeOfOld)
            .add("retainedSize", Record::getRetainedSize)
            .build();

    public static Comparator<Record> sortBy(String field, boolean ascendingOrder){
        return ascendingOrder? sortTable.get(field):sortTable.get(field).reversed();
    }

    @Override
    public Object getBySearchType(SearchType type) {
        switch (type) {
            case BY_NAME:
                return getLabel();
            case BY_OBJ_NUM:
                return getNumberOfObjects();
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