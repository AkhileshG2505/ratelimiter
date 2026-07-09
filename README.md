# Rate Limiter — Spring Boot + Redis

A rate limiting system built in Java/Spring Boot, implemented in two ways: an in-memory Token Bucket and a Redis-backed Fixed Window Counter. Built to understand rate limiting algorithms, concurrency, and distributed systems concepts from first principles.

## Why two implementations?

Rate limiting is usually taught as "add Redis and done" — this project deliberately builds it twice to understand *why* the distributed version is needed:

1. **In-memory Token Bucket** — works correctly for a single server instance, but breaks down the moment you run multiple instances behind a load balancer (each instance has its own separate counter, so the effective limit multiplies by the number of instances).
2. **Redis-backed Fixed Window Counter** — solves this by moving the counter to a single shared external store that every instance talks to, so the limit holds true regardless of how many app instances are running.

## Algorithms

### Token Bucket (in-memory)
Each client gets a bucket with a fixed capacity. Tokens refill continuously over time (not in discrete steps) based on elapsed time since the last check. Each request consumes one token; if none are available, the request is rejected.

- Smooth, continuous refill — no hard reset, so no burst-at-boundary problem.
- Uses `synchronized` to prevent race conditions between concurrent threads on the same instance.
- Limitation: state lives in a `ConcurrentHashMap` inside the JVM — not shared across multiple app instances.

### Fixed Window Counter (Redis-backed)
Each client's request count is tracked in Redis under a key like `rate_limit:<clientId>`, incremented atomically via `INCR`. On the first request in a window, an `EXPIRE` is set so the key — and the counter — resets automatically after the window closes.

- Uses Redis's atomic `INCR` to avoid race conditions across multiple app instances (not just threads).
- Simpler to implement than Token Bucket in Redis (no Lua scripting needed).
- Trade-off: allows bursts at window boundaries (e.g., a client could send requests at the end of one window and the start of the next in quick succession) — a known limitation of Fixed Window vs. Token Bucket or Sliding Window approaches.

## Architecture

```
Request → RateLimitInterceptor (preHandle)
              ↓
         extracts client IP
              ↓
         RateLimiterService / RedisRateLimiterService
              ↓
     TokenBucket (in-memory)  OR  Redis INCR + EXPIRE
              ↓
       allow → Controller runs
       reject → 429 Too Many Requests, request stops here
```

- `RateLimitInterceptor` implements Spring's `HandlerInterceptor`, intercepting every request before it reaches a controller.
- `WebConfig` registers the interceptor against all routes (`/**`).
- Per-client isolation is handled via a map (in-memory version) or per-client Redis keys (Redis version) — one client's usage never affects another's.

## Tech stack

- Java, Spring Boot (Spring Web, Spring Data Redis)
- Redis (run via Docker for local development)
- Maven

## Running locally

1. Start Redis:
   ```
   docker run -p 6379:6379 redis
   ```
2. Run the Spring Boot app.
3. Hit `GET /hello` repeatedly — first 5 requests succeed (200), subsequent requests within the window return 429 with a `Retry-After` header.

## Design decisions & trade-offs

- **Why Token Bucket first, then Redis?** Building the in-memory version first made the core rate-limiting concept (and the concurrency problem `synchronized` solves) concrete before introducing a second, harder problem (distributed state).
- **Why Fixed Window over Token Bucket for the Redis version?** Token Bucket in Redis requires Lua scripting to keep the read-calculate-write sequence atomic. Fixed Window maps directly onto Redis's native atomic `INCR` + `EXPIRE`, making it a more approachable first distributed implementation. A Lua-scripted Token Bucket is a natural next step.
- **Why per-client (IP-based) buckets instead of one global bucket?** A shared bucket would mean one client's usage could block unrelated clients — rate limiting needs to be scoped per identity, not global.

## Possible extensions

- Sliding Window Counter or Sliding Window Log for smoother rate limiting without Lua.
- True Token Bucket in Redis via Lua scripting for atomic read-modify-write.
- API-key-based client identification instead of IP (for authenticated APIs).
- Configurable limits per endpoint instead of a single global limit.
