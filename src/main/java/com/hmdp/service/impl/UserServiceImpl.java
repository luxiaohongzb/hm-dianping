package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexPatterns;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;


/**
 * 用户接口
 *
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements IUserService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     *
     * @param phone 手机号码
     * @param session 接口
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //首先校验号码的合法性
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号码无效");
        }

        //生成验证码
        String code = RandomUtil.randomNumbers(6);

//        session.setAttribute("code", code);
//
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码，此处要调api，直接log一下

        log.debug("发送验证码成功，验证码为：{}",code);
        return Result.ok();
    }


    /**
     * 用户登录功能
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号码无效");

        }
        String phone = loginForm.getPhone();
        //拿到session里的code进行校验
        String code = loginForm.getCode();
//
//        Object cacheCode = session.getAttribute("code");

        //从redis中去查找验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);


        if(cacheCode == null || !cacheCode.equals(code)){
            //报错
            return Result.fail("验证码错误");
        }

        //校验验证成功，查询是否存在用户
        User user = query().eq("phone",phone).one();

        if(user == null){
            user = createUserWithPhone(phone);
        }
        //session.setAttribute("user", user);
        //保存到redis中去
        String token = UUID.randomUUID().toString();
        //生成token并将token凭证返回给前端
        UserDTO userDTO = new UserDTO();

        BeanUtils.copyProperties(user,userDTO);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token,userMap);

        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {

        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
