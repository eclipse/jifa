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
package org.eclipse.jifa.worker.vo.heapdump.thread;

import lombok.Data;
import org.eclipse.jifa.worker.support.SortTableGenerator;
import org.eclipse.jifa.worker.vo.heapdump.histogram.Record;

import java.util.Comparator;
import java.util.Map;

@Data
public class Info {

    private int objectId;

    private String object;

    private String name;

    private long shallowSize;

    private long retainedSize;

    private String contextClassLoader;

    private boolean hasStack;

    private boolean daemon;

    public Info(int objectId, String object, String name, long shallowSize, long retainedSize,
                String contextClassLoader, boolean hasStack, boolean daemon) {
        this.objectId = objectId;
        this.object = object;
        this.name = name;
        this.shallowSize = shallowSize;
        this.retainedSize = retainedSize;
        this.contextClassLoader = contextClassLoader;
        this.hasStack = hasStack;
        this.daemon = daemon;
    }


    private static Map<String, Comparator> sortTable = new SortTableGenerator()
            .add("id", Info::getObjectId)
            .add("shallowHeap", Info::getShallowSize)
            .add("retainedHeap", Info::getRetainedSize)
            .add("daemon", Info::isDaemon)
            .add("contextClassLoader", Info::getContextClassLoader)
            .add("name",Info::getName)
            .build();

    public static Comparator sortBy(String field, boolean ascendingOrder){
        return ascendingOrder? sortTable.get(field):sortTable.get(field).reversed();
    }
}
