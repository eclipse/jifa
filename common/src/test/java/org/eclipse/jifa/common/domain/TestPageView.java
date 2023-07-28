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

import org.eclipse.jifa.common.domain.vo.PageView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPageView {

    @Test
    public void test() {
        PageView<Object> pv = PageView.empty();
        Assertions.assertNotNull(pv.getData());
        Assertions.assertEquals(0, pv.getData().size());
    }
}
