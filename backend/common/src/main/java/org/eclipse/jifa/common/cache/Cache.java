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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Cache {
    public static final Cache SINGLE=new Cache();
    public static final int DURATION = 30;
    private final com.google.common.cache.Cache<CacheKey, Object> cache;

    private Cache() {
        cache = CacheBuilder.newBuilder()
                .softValues()
                .recordStats()
                .expireAfterAccess(DURATION, TimeUnit.MINUTES)
                .build();
    }

    public static Cache getSingle(){
        return SINGLE;
    }

    @SuppressWarnings("unchecked")
    public <V> V load(CacheKey key, Callable<V> loader) {
        try {
            return (V) cache.get(key, loader);
        } catch (ExecutionException e) {
            throw new JifaException(e);
        }
    }
    //如果有一个参数与方法中参数一样，则将这个方法缓存清除，专用于AnalysisContext
    public void disposeAnalysisCacheByArg(Object arg){
        if(Objects.isNull(arg)){
            return;
        }
        Set<CacheKey> cacheKeys = new HashSet<>(cache.asMap().keySet());
        for(CacheKey cacheKey:cacheKeys){
            if(Objects.nonNull(cacheKey) && Objects.nonNull(cacheKey.args)){
                for(Object argTmp:cacheKey.args){
                    if(arg.equals(argTmp)){
                        cache.invalidate(cacheKey);
                    }
                }
            }
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
