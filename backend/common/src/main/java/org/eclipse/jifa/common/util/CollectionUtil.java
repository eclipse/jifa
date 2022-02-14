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

package org.eclipse.jifa.common.util;

import java.util.Collection;
import java.util.function.Consumer;

public class CollectionUtil {

    @SafeVarargs
    public static <T> void forEach(Consumer<T> action, Collection<? extends T>... cs) {
        for (Collection<? extends T> c : cs) {
            c.forEach(action);
        }
    }
}
