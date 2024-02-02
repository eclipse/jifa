/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.profile.model;

import lombok.Getter;
import org.eclipse.jifa.profile.enums.GraphType;
import org.eclipse.jifa.profile.enums.Unit;

@Getter
public class PerfDimension extends LeafPerfDimension {
    private final LeafPerfDimension[] subDimensions;

    public PerfDimension(String key, Desc desc, Filter[] filters, GraphType[] supportedGraphs) {
        this(key, desc, filters, Unit.COUNT, supportedGraphs);
    }

    public PerfDimension(String key, Desc desc, Filter[] filters, Unit unit, GraphType[] supportedGraphs) {
        super(key, desc, filters, unit, supportedGraphs);
        this.subDimensions = null;
    }

    public PerfDimension(String key, Desc desc, LeafPerfDimension[] subDimensions, GraphType[] supportedGraphs) {
        super(key, desc, null, null, supportedGraphs);
        this.subDimensions = subDimensions;
    }

    public static PerfDimension of(String key, String desc, Filter[] filters, GraphType[] supportedGraphs) {
        return new PerfDimension(key, Desc.of(desc), filters, supportedGraphs);
    }

    public static PerfDimension of(String key, String desc, Filter[] filters, Unit unit, GraphType[] supportedGraphs) {
        return new PerfDimension(key, Desc.of(desc), filters, unit, supportedGraphs);
    }

    public static PerfDimension of(String key, String desc, LeafPerfDimension[] subDimensions, GraphType[] supportedGraphs) {
        return new PerfDimension(key, Desc.of(desc), subDimensions, supportedGraphs);
    }
}
