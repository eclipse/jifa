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
package org.eclipse.jifa.jfr.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestDescriptorUtil {
    @Test
    public void decodeMethodArgsTest() {
        DescriptorUtil util = new DescriptorUtil();

        Assertions.assertEquals(util.decodeMethodArgs(
                        "(Ljava/lang/String;I[[ZJ)D"),
                "(String, int, boolean[][], long)"
                // "(java.lang.String, int, boolean[][], long)"
        );

        Assertions.assertEquals(util.decodeMethodArgs(
                        "(I)V"),
                "(int)"
        );

        Assertions.assertEquals(util.decodeMethodArgs(
                        "(Ljava/io/DataOutput;I)V"),
                "(DataOutput, int)"
                // "(java.io.DataOutput, int)"
        );

        Assertions.assertEquals(util.decodeMethodArgs(
                        "(Ljava/lang/Class;Ljava/util/List;Ljava/util/List;)Ljdk/jfr/EventType"),
                "(Class, List, List)"
                // "(java.lang.Class, java.util.List, java.util.List)"
        );
    }
}