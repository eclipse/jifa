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
public class PerfDimension extends LeafPerfDimension {
    private final LeafPerfDimension[] subDimensions;

    public PerfDimension(String key, Desc desc, Filter[] filters) {
        this(key, desc, filters, Unit.COUNT);
    }

    public PerfDimension(String key, Desc desc, Filter[] filters, Unit unit) {
        super(key, desc, filters, unit);
        this.subDimensions = null;
    }

    public PerfDimension(String key, Desc desc, LeafPerfDimension[] subDimensions) {
        super(key, desc, null, null);
        this.subDimensions = subDimensions;
    }

    public static PerfDimension of(String key, String desc, Filter[] filters) {
        return new PerfDimension(key, Desc.of(desc), filters);
    }

    public static PerfDimension of(String key, String desc, Filter[] filters, Unit unit) {
        return new PerfDimension(key, Desc.of(desc), filters, unit);
    }

    public static PerfDimension of(String key, String desc, LeafPerfDimension[] subDimensions) {
        return new PerfDimension(key, Desc.of(desc), subDimensions);
    }
}
