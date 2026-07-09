package com.app.ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor() {
        // 5 tokens capacity, refill 1 token/sec — same config as our earlier test
        this.rateLimiterService = new RateLimiterService(5, 1);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        System.out.println("Interceptor hit!");
        String clientId = request.getRemoteAddr(); // client's IP address

        boolean allowed = rateLimiterService.isAllowed(clientId);

        if (!allowed) {
            response.setStatus(429); // 429 Too Many Requests
            response.setHeader("Retry-After", "1"); // suggest retry after 1 second
            try {
                response.getWriter().write("Too many requests. Please try again later.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false; // STOP — don't let the request reach the controller
        }

        return true; // allowed — let it continue to the actual endpoint
    }
}
