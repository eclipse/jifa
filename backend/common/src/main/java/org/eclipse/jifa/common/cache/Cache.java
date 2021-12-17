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

import com.google.common.cache.CacheBuilder;
import org.eclipse.jifa.common.JifaException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class Cache {

    private final com.google.common.cache.Cache<CacheKey, Object> cache;

    public Cache() {
        cache = CacheBuilder
                .newBuilder()
                .softValues()
                .recordStats()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
    }

    @SuppressWarnings("unchecked")
    public <V> V load(CacheKey key, Callable<V> loader) {
        try {
            return (V) cache.get(key, loader);
        } catch (ExecutionException e) {
            throw new JifaException(e);
        }
    }

    static class CacheKey {

        Method method;

        Object[] args;

        CacheKey(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CacheKey cacheKey = (CacheKey) o;
            return method.equals(cacheKey.method) && Arrays.equals(args, cacheKey.args);
        }

        @Override
        public int hashCode() {
            int hash = method.hashCode();
            return hash * 31 ^ Arrays.hashCode(args);
        }
    }
}
