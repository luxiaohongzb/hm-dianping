package com.hmdp.service.impl;

import com.hmdp.dto.Result;
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
    public Result queryBlogById(@PathVariable Integer id){
        Blog blog = blogService.getById(id);
        if(blog == null) return Result.fail("笔记不存在");
        queryBlogUser(blog);
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long id) {
        // 修改点赞数量
        //加入redis判断是否已经点赞

        Long userId = UserHolder.getUser().getId();
        String blogKey = BLOG_LIKED_KEY + id;

        Boolean isLiked = stringRedisTemplate.opsForSet().isMember(blogKey, userId.toString());
        if(Boolean.FALSE.equals(isLiked)){
            boolean success = blogService.update()
                    .setSql("liked = liked + 1").eq("id", id).update();
            if(success) stringRedisTemplate.opsForSet().add(blogKey,userId.toString());
        }
        else {
            boolean success = blogService.update().setSql("liked = liked - 1").eq("id", id).update();
            if(success) stringRedisTemplate.opsForSet().remove(blogKey,userId.toString());
        }
        return Result.ok();
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
