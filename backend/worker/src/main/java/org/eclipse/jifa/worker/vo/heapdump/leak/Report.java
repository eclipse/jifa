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
package org.eclipse.jifa.worker.vo.heapdump.leak;

import lombok.Data;

import java.util.List;

@Data
public class Report {

    private boolean useful;

    private String info;

    private String name;

    private List<Slice> slices;

    private List<Record> records;

    @Data
    public static class Slice {

        private String label;

        private int objectId;

        private double value;

        private String desc;

        public Slice(String label, int objectId, double value, String desc) {
            this.label = label;
            this.objectId = objectId;
            this.value = value;
            this.desc = desc;
        }
    }

    @Data
    public static class Record {

        private String name;

        private String desc;

        private int index;

        private List<ShortestPath> paths;
    }

    @Data
    public static class ShortestPath {

        private String label;

        private long shallowSize;

        private long retainedSize;

        private int objectId;

        private int objectType;

        private boolean gCRoot;

        private List<ShortestPath> children;
    }
}

