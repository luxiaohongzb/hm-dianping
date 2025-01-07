package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */

@RequiredArgsConstructor
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;


    @Override
    public Result queryById(Long id) {
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if(shop == null){
            return Result.fail("店铺不存在！");
        }

//        //先从redis中去查找是否有结果
//        String hKey = CACHE_SHOP_KEY + id;
//        String shop = stringRedisTemplate.opsForValue().get(hKey);
//        //如果缓存存在
//        if(StrUtil.isNotBlank(shop)){
//            //做反序列化并返回结果
//            //log.debug("查询缓存");
//            Shop shopBean = JSONUtil.toBean(shop, Shop.class);
//            return Result.ok(shopBean);
//        }
//        if(shop == null){
//            return Result.fail("ship data not exists");
//        }
//        //解决缓存穿透问题
//        //不存在的首先尝试获取锁
//
//        boolean isLock = tryLock(LOCK_SHOP_KEY + id);
//        //缓存穿透问题
//        Shop shop1 = null;
//        try {
//            //如果没有获取到锁则等待一段时间后重新去拿数据
//            if(!isLock){
//                Thread.sleep(50);
//                queryById(id);
//            }
//            else{
//                // 查询数据库
//                 shop1 = getById(id);
//                if(shop1 == null){
//                    stringRedisTemplate.opsForValue().set(hKey,"",CACHE_NULL_TTL);
//                    return Result.ok();
//                }
//                //添加缓存并且设置过期时间
//                stringRedisTemplate.opsForValue().set(hKey, JSONUtil.toJsonStr(shop1),30L, TimeUnit.MINUTES);
//            }
//        }catch (Exception e){
//            throw new RuntimeException(e);
//        }finally {
//            unLock(LOCK_SHOP_KEY + id);
//        }
        return Result.ok(shop);
    }

    public void saveShop2Redis(Long id,Long expireSeconds){
        Shop shop = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }


    /**
     * 藏尸给查询线程上锁
     * @param key lock key
     * @return returns true if lock successfully and false is the lock is hold by other thread
     */
    private boolean tryLock(String key){
        Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(isLock);
    }

    /**
     * 给查询线程解锁
     * @param key
     */
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
    /**
     * 更新商店数据
     * @param shop
     * @return
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        //首先拿出id

        Long id = shop.getId();
        //判断id
        if(id == null){
            return Result.fail("店铺id不能为空！");
        }
        //尝试去更新店铺
         updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
