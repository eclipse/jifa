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
package org.eclipse.jifa.master.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

public class ProxyDictionary {

    private static Map<String, Object> proxyMap = new ConcurrentHashMap<>();

    static synchronized void add(Class<?> serviceInterface, Object proxy) {
        proxyMap.put(serviceInterface.getSimpleName(), proxy);
    }

    @SuppressWarnings("unchecked")
    public static <T> T lookup(Class<?> key) {
        Object proxy = proxyMap.get(key.getSimpleName());
        ASSERT.notNull(proxy);
        return (T) proxy;
    }
}
