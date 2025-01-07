package com.hmdp;


import com.hmdp.service.IShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class TestRedis {
  //  @Autowired
//    private RedisTemplate redisTemplate;
    @Autowired
    IShopService shopService;
//    @Test
//    void testString(){
//        stringRedisTemplate.opsForValue().set("hello","world");
//        Object name  = redisTemplate.opsForValue().get("hello");
//        System.out.printf("name:%s",name);
//    }

    @Test
    void testSaveShop(){
        shopService.saveShop2Redis(1L,20L);
    }

}
