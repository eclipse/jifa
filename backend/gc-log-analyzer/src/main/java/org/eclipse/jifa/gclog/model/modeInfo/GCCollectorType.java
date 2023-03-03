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

package org.eclipse.jifa.gclog.model.modeInfo;

public enum GCCollectorType {
    EPSILON("Epsilon GC"),
    SERIAL("Serial GC"),
    PARALLEL("Parallel GC"),
    ZGC("ZGC"),
    GENZ("Generational ZGC"),
    SHENANDOAH("Shenandoah GC"),
    GENSHEN("Generational Shenandoah GC"),
    G1("G1 GC"),
    CMS("CMS GC"),
    UNKNOWN("Unknown GC");

    private String name;

    GCCollectorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
