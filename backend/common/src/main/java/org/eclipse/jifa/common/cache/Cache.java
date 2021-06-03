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
