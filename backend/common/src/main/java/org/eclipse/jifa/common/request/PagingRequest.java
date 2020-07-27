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
package org.eclipse.jifa.common.request;

import lombok.Data;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

@Data
public class PagingRequest {

    // start with 1
    private int page;

    private int pageSize;

    public PagingRequest(int page, int pageSize) {
        ASSERT.isTrue(page >= 1 && pageSize >= 1);
        this.page = page;
        this.pageSize = pageSize;
    }

    public int from() {
        return (page - 1) * pageSize;
    }

    public int to(int totalSize) {
        return Math.min(from() + pageSize, totalSize);
    }
}
