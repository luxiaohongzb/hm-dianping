package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Title: SimpleRedisLock
 * @Author mingliu0608
 * @Package com.hmdp.utils
 * @Date 2025/1/9 16:29
 * @description: redisLock
 */



public class SimpleRedisLock implements ILock {

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString()+"-";

    StringRedisTemplate stringRedisTemplate;
    String userKey;

    public SimpleRedisLock(String userKey,StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.userKey = userKey;
    }

    /**
     * 尝试获取锁
     *
     * @param timeoutSec 锁持有的超时时间，过期后自动释放
     * @return true代表获取锁成功; false代表获取锁失败
     */
    @Override
    public boolean tryLock(long timeoutSec) {
        long threadId =  Thread.currentThread().getId();
        //可重入
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + userKey, ID_PREFIX + threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
      //  stringRedisTemplate.opsForValue().setIfAbsent();
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        //可重入锁
        //检查锁中的标识
        String threadId = ID_PREFIX + Thread.currentThread().getId() ;
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + userKey);
        //
        if(threadId.equals(id)){
            stringRedisTemplate.delete(KEY_PREFIX + userKey );
        }
    }
}
