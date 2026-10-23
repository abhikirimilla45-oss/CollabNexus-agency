package com.collabnexus.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory rate limiter for authentication endpoints. This protects a
 * single instance; a clustered deployment should replace it with a shared store
 * such as Redis or a gateway-level limiter.
 */
@Component
public class AuthRateLimiter {

    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();
    private final boolean enabled;

    public AuthRateLimiter(@Value("${cn.security.rate-limit.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public void check(String key, int limit, Duration window) {
        if (!enabled) {
            return;
        }
        long now = Instant.now().toEpochMilli();
        long cutoff = now - window.toMillis();
        Deque<Long> timestamps = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                throw com.collabnexus.common.Exceptions.tooManyRequests(
                        "Too many attempts. Please wait a few minutes and try again.");
            }
            timestamps.addLast(now);
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}
