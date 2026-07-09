package com.app.ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RedisRateLimiterService redisRateLimiterService;

    public RateLimitInterceptor(RedisRateLimiterService redisRateLimiterService) {
        this.redisRateLimiterService = redisRateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        System.out.println("Interceptor hit!");
        String clientId = request.getRemoteAddr(); // client's IP address

        boolean allowed = redisRateLimiterService.isAllowed(clientId);

        if (!allowed) {
            response.setStatus(429); // 429 Too Many Requests
            response.setHeader("Retry-After", "60"); // suggest retry after 60 second
            try {
                response.getWriter().write("Too many requests. Please try again later after 60 seconds .");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false; // STOP — don't let the request reach the controller
        }

        return true; // allowed — let it continue to the actual endpoint
    }
}
