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

package org.eclipse.jifa.gclog.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class I18nStringView {
    private String name;// should be i18n name in frontend
    private Map<String, Object> params;

    public I18nStringView(String name) {
        this.name = name;
    }

    public I18nStringView(String name, Object... params) {
        this.name = name;
        if (params.length % 2 != 0) {
            throw new RuntimeException("params number should be multiple of 2");
        }
        if (params.length == 0) {
            return;
        }
        this.params = new HashMap<>();
        for (int i = 0; i < params.length; i += 2) {
            this.params.put(params[i].toString(), params[i + 1]);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        I18nStringView that = (I18nStringView) o;
        return Objects.equals(name, that.name) && Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, params);
    }
}
