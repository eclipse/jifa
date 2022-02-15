/********************************************************************************
 * Copyright (c) 2021, 2022 Contributors to the Eclipse Foundation
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

    private static <T> Enhancer buildEnhancer(Class<T> clazz) {
        Enhancer e = new Enhancer();
        e.setSuperclass(clazz);
        e.setCallback(new Handler(clazz));
        return e;
    }

    @SuppressWarnings("unchecked")
    public static <T> T build(Class<T> clazz) {
        return (T) buildEnhancer(clazz).create();
    }

    @SuppressWarnings("unchecked")
    public static <T> T build(Class<T> clazz, Class<?>[] argTypes, Object[] args) {
        return (T) buildEnhancer(clazz).create(argTypes, args);
    }
}
