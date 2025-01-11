package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.FOLLOWED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    IUserService userService;
    @Override
    public Result follow(Long followedUserId, boolean isFollowed) {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        if(userId == null) {
            return Result.fail("用户未登录！");
        }
        if(isFollowed){
            //关注用户
            Follow follow = new Follow();
            follow.setFollowUserId(followedUserId);
            follow.setUserId(userId);
            boolean save = save(follow);


        }
        else {
            remove(new QueryWrapper<Follow>()
                .eq("user_id",userId).eq("follow_user_id",followedUserId));
        }

        return Result.ok();
    }

    @Override
    public Result isFollowed(Long followedUserId) {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        if(userId == null) {
            return Result.fail("用户未登录！");
        }
        Integer count = query()
                .eq("user_id", userId)
                .eq("follow_user_id", followedUserId)
                .count();

        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long targetUserId) {

        //得到对方用户的关注列表，和用户的关注列表去求交集
        Long userId = UserHolder.getUser().getId();
        if(userId == null) {
            return Result.fail("用户未登录！");
        }

        String key1 = FOLLOWED_KEY + targetUserId;
        String key2 = FOLLOWED_KEY + userId;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if(intersect == null || intersect.isEmpty()){
            return  Result.ok(Collections.emptyList());
        }
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        List<User> users = userService.listByIds(ids);
        List<UserDTO> userDTOS = BeanUtil.copyToList(users, UserDTO.class);
        return Result.ok(userDTOS);

    }
}
