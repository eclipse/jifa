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

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.eclipse.jifa.common.JifaException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class Handler implements MethodInterceptor {

    private final Cache cache;

    private final List<Method> cacheableMethods;

    public Handler(Class<?> target) {
        cache = new Cache();
        cacheableMethods = new ArrayList<>();

        try {
            Method[] methods = target.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getAnnotation(Cacheable.class) != null) {
                    method.setAccessible(true);
                    int mod = method.getModifiers();
                    if (Modifier.isAbstract(mod) || Modifier.isFinal(mod) ||
                        !(Modifier.isPublic(mod) || Modifier.isProtected(mod))) {
                        throw new JifaException("Illegal method modifier: " + method);
                    }
                    cacheableMethods.add(method);
                }
            }
        } catch (Exception exception) {
            throw new JifaException(exception);
        }
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        if (cacheableMethods.contains(method)) {
            return cache.load(new Cache.CacheKey(method, args),
                              () -> {
                                  try {
                                      return proxy.invokeSuper(obj, args);
                                  } catch (Throwable throwable) {
                                      if (throwable instanceof RuntimeException) {
                                          throw (RuntimeException) throwable;
                                      }
                                      throw new JifaException(throwable);
                                  }
                              });
        }
        return proxy.invokeSuper(obj, args);
    }
}
