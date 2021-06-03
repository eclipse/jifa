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
import org.eclipse.jifa.common.aux.JifaException;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

class Cache {

    private final com.google.common.cache.Cache<CacheKey, Object> cache;

    public Cache() {
        cache = CacheBuilder.newBuilder().build();
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

        Object[] keys;

        CacheKey(Object[] keys) {
            this.keys = keys;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CacheKey cacheKey = (CacheKey) o;
            return Arrays.equals(keys, cacheKey.keys);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(keys);
        }
    }
}
