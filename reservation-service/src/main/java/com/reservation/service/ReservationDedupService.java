package com.reservation.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ReservationDedupService {
    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquire(String key, String value, long ttlSec) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttlSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(result);
    }
}