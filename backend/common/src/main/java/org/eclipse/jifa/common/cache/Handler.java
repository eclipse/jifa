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

import org.eclipse.jifa.common.aux.JifaException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

class Handler implements InvocationHandler {

    private final Object target;

    private final Cache cache;

    private final Map<String, Entry> map;

    public Handler(Object target) {
        this.target = target;
        cache = new Cache();
        map = new HashMap<>();

        Method[] methods = target.getClass().getMethods();
        try {
            Map<String, Method> providers = new HashMap<>();
            Map<String, Method> posters = new HashMap<>();

            for (Method method : methods) {
                CacheProvider provider = method.getAnnotation(CacheProvider.class);
                if (provider != null) {
                    String providerTarget = provider.target();
                    if (providerTarget.isEmpty()) {
                        int i = method.getName().indexOf("CacheProvider");
                        if (i == -1) {
                            throw new JifaException("Method name must end withs CacheProvider: " + method);
                        }
                        providerTarget = method.getName().substring(0, i);
                    }
                    if (providers.containsKey(providerTarget)) {
                        throw new JifaException("Duplicated provider");
                    }
                    providers.put(providerTarget, method);
                }

                CachePoster poster = method.getAnnotation(CachePoster.class);
                if (poster != null) {
                    String posterTarget = poster.target();
                    if (posterTarget.isEmpty()) {
                        int i = method.getName().indexOf("CachePoster");
                        if (i == -1) {
                            throw new JifaException("Method name must end withs CachePoster: " + method);
                        }
                        posterTarget = method.getName().substring(0, i);
                    }

                    if (posters.containsKey(posterTarget)) {
                        throw new JifaException("Duplicated poster");
                    }
                    posters.put(posterTarget, method);
                }
            }

            for (Method method : methods) {
                Cacheable c = method.getAnnotation(Cacheable.class);
                if (c != null) {
                    String key = methodKey(method);
                    Entry entry = new Entry();
                    entry.key = key;

                    Method provider = providers.get(key);
                    if (provider == null) {
                        throw new JifaException("Provider must be exist: " + key);
                    }

                    entry.provider = provider;
                    provider.setAccessible(true);
                    Parameter[] allParams = method.getParameters();
                    Parameter[] providerParams = provider.getParameters();
                    int[] indices = new int[providerParams.length];
                    for (int i = 0; i < providerParams.length; i++) {
                        String name = providerParams[i].getName();
                        assert !name.startsWith("args");
                        for (int j = 0; j < allParams.length; j++) {
                            if (name.equals(allParams[j].getName())) {
                                indices[i] = j;
                            }
                        }
                    }
                    entry.providerArgIndices = indices;

                    Method poster = posters.get(key);
                    if (poster != null) {
                        entry.poster = poster;
                        poster.setAccessible(true);

                        Parameter[] posterParams = poster.getParameters();
                        indices = new int[posterParams.length];
                        // index 0 is for provider result
                        for (int i = 1; i < posterParams.length; i++) {
                            String name = posterParams[i].getName();
                            assert !name.startsWith("args");
                            for (int j = 0; j < allParams.length; j++) {
                                if (name.equals(allParams[j].getName())) {
                                    indices[i] = j;
                                }
                            }
                        }
                        entry.posterArgIndices = indices;
                    }

                    map.put(key, entry);
                }
            }
        } catch (Exception exception) {
            throw new JifaException(exception);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String key = methodKey(method);
        Entry entry = map.get(key);
        if (entry == null) {
            return method.invoke(target, args);
        }
        Object[] providerArgs = new Object[entry.providerArgIndices.length];
        int index = 0;
        for (int i : entry.providerArgIndices) {
            providerArgs[index++] = args[i];
        }
        Object result = cache.load(new Cache.CacheKey(key, providerArgs),
                                   () -> entry.provider.invoke(target, providerArgs));

        Method poster = entry.poster;
        if (poster != null) {
            Object[] posterArgs = new Object[entry.posterArgIndices.length];
            posterArgs[0] = result;
            for (int i = 1; i < posterArgs.length; i++) {
                posterArgs[i] = args[entry.posterArgIndices[i]];
            }
            result = poster.invoke(target, posterArgs);
        }
        return result;
    }

    private String methodKey(Method method) {
        return method.getName();
    }

    static class Entry {

        String key;

        Method provider;
        int[] providerArgIndices;

        Method poster;
        int[] posterArgIndices;
    }
}
