package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    IUserService userService;
    @Autowired
    IBlogService blogService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    public Result queryBlogById(Long id){
        Blog blog = blogService.getById(id);
        if(blog == null) return Result.fail("笔记不存在");
        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long id) {
        // 修改点赞数量
        //加入redis判断是否已经点赞

        Long userId = UserHolder.getUser().getId();
        String blogKey = BLOG_LIKED_KEY + id;

        Double score = stringRedisTemplate.opsForZSet().score(blogKey, userId.toString());
        if(score == null){
            //如果没有评论
            boolean success = blogService.update()
                    .setSql("liked = liked + 1").eq("id", id).update();
            //将当前的时间作为score
            if(success) stringRedisTemplate.opsForZSet().add(blogKey,userId.toString(),System.currentTimeMillis());
        }
        else {

            //如果点赞
            boolean success = blogService.update().setSql("liked = liked - 1").eq("id", id).update();
            if(success) stringRedisTemplate.opsForZSet().remove(blogKey,userId);
        }
        return Result.ok();
    }

    /**
     * 查询用户点赞列表
     * @param id blog id
     * @return
     */
    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(top5 == null || top5.isEmpty() ){
            return  Result.ok(Collections.emptyList());
        }

        // select * from user where id in () order by
        List<Long> userIds = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idsStr = StrUtil.join(",",userIds);
        // 3.根据用户id查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
        List<UserDTO> userDTOS = userService.query()
                .in("id", userIds ).last("ORDER BY FIELD(id," +idsStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        //
        return Result.ok(userDTOS);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
    private void isBlogLiked(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        // 2.判断当前登录用户是否已经点赞
        String key = "blog:liked:" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

}
