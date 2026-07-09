package com.app.ratelimiter;

import com.app.ratelimiter.TokenBucket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class RateLimiterService {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private final int capacity;
    private final double refillRatePerSecond;

    public RateLimiterService(int capacity, double refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    public boolean isAllowed(String clientId) {
        TokenBucket bucket = buckets.computeIfAbsent(clientId,
                key -> new TokenBucket(capacity, refillRatePerSecond));
        boolean result = bucket.tryConsume();
        System.out.println("Client: " + clientId + " | Allowed: " + result);
        return bucket.tryConsume();
    }
}