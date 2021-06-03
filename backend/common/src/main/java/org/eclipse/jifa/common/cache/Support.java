package org.eclipse.jifa.common.cache;

import java.lang.reflect.Proxy;

public class Support {

    @SuppressWarnings("unchecked")
    public static <T> T buildProxy(T target) {
        return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(),
                                          target.getClass().getInterfaces(),
                                          new Handler(target));
    }
}
