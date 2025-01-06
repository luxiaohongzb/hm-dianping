package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

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



    @Override
    public Result queryById(Long id) {

        //先从redis中去查找是否有结果
        String hKey = CACHE_SHOP_KEY + id;
        String shop = stringRedisTemplate.opsForValue().get(hKey);
        //如果缓存存在
        if(StrUtil.isNotBlank(shop)){
            //做反序列化并返回结果
            log.debug("查询缓存");
            Shop shopBean = JSONUtil.toBean(shop, Shop.class);
            return Result.ok(shopBean);
        }
        //不存在的话直接查询数据库
        Shop shop1 = getById(id);
        if(shop1 == null){
            return Result.ok();
        }
        stringRedisTemplate.opsForValue().set(hKey, JSONUtil.toJsonStr(shop1));
        return Result.ok(shop1);
    }
}
