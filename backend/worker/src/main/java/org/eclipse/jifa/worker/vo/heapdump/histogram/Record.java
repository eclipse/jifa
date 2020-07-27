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

@Data
public class Record {
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

    public static class Type {
        public static int CLASS = 1;
        public static int SUPER_CLASS = 2;
        public static int CLASS_LOADER = 3;
        public static int PACKAGE = 4;
    }
}
