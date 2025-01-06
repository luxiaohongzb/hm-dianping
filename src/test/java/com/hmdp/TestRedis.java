package com.hmdp;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class TestRedis {
    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void testString(){
        redisTemplate.opsForValue().set("hello","world");
        Object name  = redisTemplate.opsForValue().get("hello");
        System.out.printf("name:%s",name);
    }

}
