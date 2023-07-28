/********************************************************************************
 * Copyright (c) 2020, 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.common.domain.vo;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.common.domain.request.PagingRequest;

import java.util.Collections;
import java.util.List;

/**
 * A page view is a page of data with paging information
 *
 * @param <T> the type of data
 */
@Getter
@Setter
public class PageView<T> {

    private static final PageView<?> EMPTY = new PageView<>(null, 0, Collections.emptyList());

    /**
     * Return an empty page view.
     *
     * @param <T> data type
     * @return empty page view
     */
    @SuppressWarnings("unchecked")
    public static <T> PageView<T> empty() {
        return (PageView<T>) EMPTY;
    }

    private List<T> data;

    private int page;

    private int pageSize;

    private int totalSize;

    /**
     * Create a page view with paging request, total size and data.
     *
     * @param request   paging request
     * @param totalSize total size
     * @param data      data
     */
    public PageView(PagingRequest request, int totalSize, List<T> data) {
        this.data = data;
        this.page = request != null ? request.getPage() : 0;
        this.pageSize = request != null ? request.getPageSize() : 0;
        this.totalSize = totalSize;
    }

    /**
     * Create a page view with page index, page size, total size and data.
     *
     * @param page      page index
     * @param pageSize  page size
     * @param totalSize total size
     * @param data      data
     */
    public PageView(int page, int pageSize, int totalSize, List<T> data) {
        this.data = data;
        this.page = page;
        this.pageSize = pageSize;
        this.totalSize = totalSize;
    }
}
