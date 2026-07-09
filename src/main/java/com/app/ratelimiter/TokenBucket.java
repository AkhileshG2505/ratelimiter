package com.app.ratelimiter;

    public class TokenBucket {
        private final int capacity;
        private double tokens;
        private final double refillRatePerSecond;
        private long lastRefillTimestamp;

        public TokenBucket(int capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.lastRefillTimestamp = System.nanoTime();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double secondsElapsed = (now - lastRefillTimestamp) / 1_000_000_000.0;
            double tokensToAdd = secondsElapsed * refillRatePerSecond;

            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTimestamp = now;
            }
        }
    }

