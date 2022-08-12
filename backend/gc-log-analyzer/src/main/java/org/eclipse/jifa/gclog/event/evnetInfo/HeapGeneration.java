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

public enum HeapGeneration {
    YOUNG,
    EDEN,
    SURVIVOR,
    OLD,
    METASPACE, // also represents perm
    HUMONGOUS,
    TOTAL; //young + old + humongous

    public static HeapGeneration getHeapGeneration(String name) {
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
            case "humongous":
                return HUMONGOUS;
            case "total":
            case "heap":
                return TOTAL;
            default:
                return null;
        }
    }

}
