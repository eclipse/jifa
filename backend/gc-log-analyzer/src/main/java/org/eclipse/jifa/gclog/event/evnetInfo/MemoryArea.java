/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.gclog.event.evnetInfo;

public enum MemoryArea {
    EDEN("eden"),
    SURVIVOR("survivor"),
    YOUNG("young"),
    OLD("old"),
    HUMONGOUS("humongous"),
    ARCHIVE("archive"),
    HEAP("heap"), //young + old + humongous
    METASPACE("metaspace"), // also represents perm
    CLASS("class"),
    NONCLASS("nonclass");

    public static MemoryArea getMemoryArea(String name) {
        if (name == null) {
            return null;
        }
        switch (name.trim().toLowerCase()) {
            case "young":
            case "parnew":
            case "defnew":
            case "psyounggen":
                return YOUNG;
            case "eden":
                return EDEN;
            case "survivor":
            case "survivors":
                return SURVIVOR;
            case "tenured":
            case "old":
            case "psoldgen":
            case "paroldgen":
            case "cms":
            case "ascms":
                return OLD;
            case "metaspace":
            case "perm":
                return METASPACE;
            case "class":
                return CLASS;
            case "nonclass":
                return NONCLASS;
            case "humongous":
                return HUMONGOUS;
            case "archive":
                return ARCHIVE;
            case "total":
            case "heap":
                return HEAP;
            default:
                return null;
        }
    }

    private final String name;

    MemoryArea(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
