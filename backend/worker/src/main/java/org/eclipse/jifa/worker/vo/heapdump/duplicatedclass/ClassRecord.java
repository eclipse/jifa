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
package org.eclipse.jifa.worker.vo.heapdump.duplicatedclass;

import lombok.Data;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.worker.vo.feature.SearchType;
import org.eclipse.jifa.worker.vo.feature.Searchable;

@Data
public class ClassRecord implements Searchable {

    private String label;

    private int count;

    @Override
    public Object getBySearchType(SearchType type) {
        switch (type) {
            case BY_NAME:
                return getLabel();
            case BY_CLASSLOADER_COUNT:
                // Type cast is necessary since its caller accepts long type only
                return Long.valueOf(getCount());
            default:
                ErrorUtil.shouldNotReachHere();
        }
        return null;
    }
}
