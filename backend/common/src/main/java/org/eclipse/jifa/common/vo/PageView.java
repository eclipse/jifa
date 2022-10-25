/********************************************************************************
 * Copyright (c) 2020, 2021 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.common.vo;

import lombok.Data;
import org.eclipse.jifa.common.request.PagingRequest;

import java.util.Collections;
import java.util.List;

@Data
public class PageView<T> {

    public static final PageView<?> EMPTY = new PageView<>(null, 0, Collections.emptyList());

    @SuppressWarnings("unchecked")
    public static <T> PageView<T> empty() {
        return (PageView<T>) EMPTY;
    }

    private List<T> data;

    private int page;

    private int pageSize;

    private int totalSize;

    private int filtered;

    public PageView(PagingRequest request, int totalSize, List<T> data) {
        this.data = data;
        this.page = request != null ? request.getPage() : 0;
        this.pageSize = request != null ? request.getPageSize() : 0;
        this.totalSize = totalSize;
    }

    public PageView() {
    }

}
