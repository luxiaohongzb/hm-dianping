package com.hmdp.utils;


/**
 *
 * @Author mingliu0608
 */

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;


//基于StringRedisTemplate封装一个缓存工具类，满足下列需求：
//
//方法1：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
//方法2：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓
@Slf4j
@Component
public class CacheClient {
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    private  final StringRedisTemplate stringRedisTemplate;
    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }
    /**
     *
     * 将java对象序列化为json并且存储在key中
     *
     * @param key redis key
     * @param value redis value
     * @param ttl ttl
     * @param timeUnit  timeUnit
     */
    public void set(String key, Object value, Long ttl, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),ttl,timeUnit);
    }
    /**
     *
     * 将java对象序列化为json,在数据中设置一个逻辑过期时间
     *
     * @param key redis key
     * @param value redis value
     * @param ttl ttl
     * @param timeUnit  timeUnit
     */
    public void setWithLoginExpire(String key, Object value, Long ttl, TimeUnit timeUnit){
        //给定一个逻辑过期的时间
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(ttl)));
        redisData.setData(value);
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);

        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json,type);
        }
        // 判断命中的是否是空值
        if (json != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        // 5.不存在，返回错误
        if (r == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 6.存在，写入redis
        this.set(key, r, time, unit);
        return r;

    }
    /**
     * 查询逻辑过期时间，如果没有过期直接返回结果，如果过期了就开新线程去重建缓存
     * @param keyPrefix key_prefix
     * @param id key_id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @return
     * @param <R>
     * @param <ID>
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isBlank(json)) {
            // 3.存在，直接返回
            return null;
        }
        // 4.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())) {
            // 5.1.未过期，直接返回店铺信息
            return r;
        }
        // 5.2.已过期，需要缓存重建
        // 6.缓存重建
        // 6.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2.判断是否获取锁成功
        if (isLock){
            // 6.3.成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R newR = dbFallback.apply(id);
                    // 重建缓存
                    this.setWithLoginExpire(key, newR, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }
        // 6.4.返回过期的商铺信息
        return r;
    }


    /**
     *
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param timeUnit
     * @return
     * @param <R>
     * @param <ID>
     */
    public <R,ID> R queryWithMuteX(String keyPrefix,ID id,Class<R> type,Function<ID,R> dbFallback,Long time,TimeUnit timeUnit){
            String key = keyPrefix + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //如果有的化就直接去返回
        if(StrUtil.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson,type);
        }
        if(shopJson != null){
            return null;
        }
        String lockKey = LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            if(!isLock){
                //获取锁失败，休眠再去重试
                Thread.sleep(50);
                //try again
                return queryWithMuteX(keyPrefix, id, type, dbFallback, time, timeUnit);
            }
            else {
                //获取锁成功，尝试重建缓存
                r = dbFallback.apply(id);
            }
            if(r == null){
                //返回结果是null的话，直接返回null值防止缓存穿透
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            this.set(key,r,time,timeUnit);
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }

        finally {
            unlock(lockKey);
        }
        return r;
    }

    /**
     * 尝试加锁
     * @param lockKey 加锁
     * @return
     */
    private boolean tryLock(String lockKey) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);

    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}


















