package org.eclipse.jifa.gclog.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Key2ValueListMap<K, V> {
    private Map<K, List<V>> map;

    public Key2ValueListMap(Map<K, List<V>> map) {
        this.map = map;
    }

    public Key2ValueListMap() {
        map = new HashMap<>();
    }

    public void put(K key, V value) {
        List<V> list = map.getOrDefault(key, null);
        if (list == null) {
            list = new ArrayList<>();
            list.add(value);
            map.put(key, list);
        }
    }

    public List<V> get(K key) {
        return map.getOrDefault(key, null);
    }

    public Map<K, List<V>> getInnerMap() {
        return map;
    }
}
