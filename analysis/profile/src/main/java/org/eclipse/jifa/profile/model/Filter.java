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

@Getter
public class Filter {
    private final String key;

    private final Desc desc;

    public Filter(String key, Desc desc) {
        this.key = key;
        this.desc = desc;
    }

    public static Filter of(String key, String desc) {
        return new Filter(key, Desc.of(desc));
    }
}
