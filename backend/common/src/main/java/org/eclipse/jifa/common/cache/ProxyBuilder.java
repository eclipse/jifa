/********************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.common.cache;

import net.sf.cglib.proxy.Enhancer;

public class ProxyBuilder {

    @SuppressWarnings("unchecked")
    public static <T> T build(T target) {
        Enhancer e = new Enhancer();
        e.setSuperclass(target.getClass());
        e.setCallback(new Handler(target));
        return (T) e.create();
    }
}
