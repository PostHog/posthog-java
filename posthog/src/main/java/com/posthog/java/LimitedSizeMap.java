package com.posthog.java;

import java.util.LinkedHashMap;
import java.util.Map;

class LimitedSizeMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    LimitedSizeMap(int maxSize) {
        super(maxSize, 0.75f, false);
        this.maxSize = maxSize;
    }

    @Override
    public V put(K key, V value) {
        if (size() >= this.maxSize) {
            clear();
        }
        return super.put(key, value);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return false; // We handle the removal logic in the put method
    }
}
