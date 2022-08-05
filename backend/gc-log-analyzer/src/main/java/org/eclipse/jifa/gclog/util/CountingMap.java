package org.eclipse.jifa.gclog.util;

import java.util.HashMap;
import java.util.Map;

public class CountingMap<T> {
    private Map<T, Integer> map = new HashMap<>();

    public void put(T key) {
        put(key, 1);
    }

    public void put(T key, int n) {
        map.put(key, map.getOrDefault(key, 0) + n);
    }

    public boolean containKey(T key) {
        return map.containsKey(key);
    }

    public int get(T key) {
        return map.getOrDefault(key, 0);
    }
}
