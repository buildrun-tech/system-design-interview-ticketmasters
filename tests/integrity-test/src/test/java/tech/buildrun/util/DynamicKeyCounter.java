package tech.buildrun.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class DynamicKeyCounter {
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();

    public void increment(String key) {
        counts.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    public long get(String key) {
        LongAdder adder = counts.get(key);
        return adder == null ? 0 : adder.sum();
    }

    public ConcurrentHashMap<String, LongAdder> getCounts() {
        return counts;
    }
}