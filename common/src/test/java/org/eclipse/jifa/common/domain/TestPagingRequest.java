/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.common.domain;

import org.eclipse.jifa.common.domain.exception.ValidationException;
import org.eclipse.jifa.common.domain.request.PagingRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPagingRequest {

    @Test
    public void test() {
        PagingRequest pagingRequest = new PagingRequest(2, 10);

        Assertions.assertEquals(10, pagingRequest.from());
        Assertions.assertEquals(20, pagingRequest.to(21));
        Assertions.assertEquals(16, pagingRequest.to(16));

        Assertions.assertThrowsExactly(ValidationException.class, () -> new PagingRequest(0, 10));
    }
}
