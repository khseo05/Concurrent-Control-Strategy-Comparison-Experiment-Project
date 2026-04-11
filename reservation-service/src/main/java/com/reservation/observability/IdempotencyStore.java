package com.reservation.observability;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyStore {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public String get(String key) {
        return store.get(key);
    }

    public boolean saveIfAbsent(String key, String value) {
        return store.putIfAbsent(key, value) == null;
    }
}