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
package org.eclipse.jifa.common.domain.request;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jifa.common.util.Validate;

/**
 * Paging request
 */
@Getter
public class PagingRequest {

    // page index starts with 1
    private int page;

    // page size, must be greater than 0
    private int pageSize;

    /**
     * Create a new PagingRequest
     *
     * @param page     page index, starts with 1
     * @param pageSize page size, must be greater than 0
     */
    public PagingRequest(int page, int pageSize) {
        Validate.isTrue(page >= 1 && pageSize >= 1);
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * @return from index (inclusive), starts with 0
     */
    public int from() {
        return (page - 1) * pageSize;
    }

    /**
     * @param totalSize total size of the elements
     * @return end index (exclusive)
     */
    public int to(int totalSize) {
        return Math.min(from() + pageSize, totalSize);
    }
}
