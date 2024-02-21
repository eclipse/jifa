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
package org.eclipse.jifa.jfr.model;

import lombok.Getter;
import org.eclipse.jifa.jfr.enums.Unit;

@Getter
public class LeafPerfDimension {
    private final String key;

    private final Desc desc;

    private final Filter[] filters;

    private final Unit unit;

    public LeafPerfDimension(String key, Desc desc, Filter[] filters, Unit unit) {
        this.key = key;
        this.desc = desc;
        this.filters = filters;
        this.unit = unit;
    }

    public LeafPerfDimension(String key, Desc desc, Filter[] filters) {
        this(key, desc, filters, null);
    }

    public static LeafPerfDimension of(String key, String desc, Filter[] filters) {
        return new LeafPerfDimension(key, Desc.of(desc), filters);
    }
}
