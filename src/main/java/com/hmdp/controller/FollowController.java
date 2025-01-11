package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Autowired
    IFollowService followService;
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long userId, @PathVariable("isFollow") boolean isFollowed){
        return followService.follow(userId,isFollowed);
    }
    @GetMapping("/or/not/{id}")
    public Result isFollowed(@PathVariable("id") Long followedUserId){
        return followService.isFollowed(followedUserId);
    }
    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long targetUserId){
        return followService.followCommons(targetUserId);
    }
}
