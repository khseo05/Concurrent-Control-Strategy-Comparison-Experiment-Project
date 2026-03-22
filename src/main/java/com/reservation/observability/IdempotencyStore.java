package com.reservation.observability;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyStore {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public String get(String key) {
        return store.get(key);
    }

    public void save(String key, String value) {
        store.put(key, value);
    }
}