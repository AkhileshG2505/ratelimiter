package com.app.ratelimiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/hello")
    public String hello() {
        return "Hello! Request succeeded.";
    }

    @GetMapping("/redis-test")
    public String redisTest() {
        redisTemplate.opsForValue().set("testKey", "It works!");
        String value = redisTemplate.opsForValue().get("testKey");
        return "Redis says: " + value;
    }
}