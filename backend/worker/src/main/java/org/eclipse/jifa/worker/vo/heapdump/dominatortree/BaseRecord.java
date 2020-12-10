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
import org.eclipse.jifa.worker.support.SortTableGenerator;

import java.util.Comparator;
import java.util.Map;

@Data
public class BaseRecord {

    private static Map<String, Comparator> sortTable = new SortTableGenerator()
            .add("id", BaseRecord::getObjectId)
            .add("shallowHeap", BaseRecord::getShallowSize)
            .add("retainedHeap", BaseRecord::getRetainedSize)
            .add("percent", BaseRecord::getPercent)
            .build();
    private String label;

    private String suffix;

    private int objectId;

    private int objectType;

    private boolean gCRoot;

    private long shallowSize;

    private long retainedSize;

    private double percent;

    private boolean isObjType = true;

    public static Comparator sortBy(String field, boolean ascendingOrder) {
        return ascendingOrder ? sortTable.get(field) : sortTable.get(field).reversed();
    }
}
