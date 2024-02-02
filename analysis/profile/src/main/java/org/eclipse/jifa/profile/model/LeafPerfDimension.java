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
public class LeafPerfDimension {
    private final String key;

    private final Desc desc;

    private final GraphType[] supportedGraphs;

    private final Filter[] filters;

    private final Unit unit;

    public LeafPerfDimension(String key, Desc desc, Filter[] filters, Unit unit, GraphType[] supportedGraphs) {
        this.key = key;
        this.desc = desc;
        this.filters = filters;
        this.unit = unit;
        this.supportedGraphs = supportedGraphs;
    }

    public LeafPerfDimension(String key, Desc desc, Filter[] filters, GraphType[] supportedGraphs) {
        this(key, desc, filters, null, supportedGraphs);
    }

    public static LeafPerfDimension of(String key, String desc, Filter[] filters, GraphType[] supportedGraphs) {
        return new LeafPerfDimension(key, Desc.of(desc), filters, supportedGraphs);
    }
}
