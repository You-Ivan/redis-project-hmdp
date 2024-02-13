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
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result sendCode(String phone) {
        // 1. verify the phone number
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }
        // 2. generate verification code
        String code = RandomUtil.randomNumbers(6);
        // 3. store the verification code in the redis phone:code
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, Duration.ofMinutes(LOGIN_CODE_TTL));
        // 4. send code to phone number
        log.info("Send Verification code: " + code + " to phone number: " + phone);
        // 5. return ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        // 1. verify the phone number
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }

        // 2. retrieve verification code from redis
        String expectedCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        // 3. return error if the code does not match
        String actualCode = loginForm.getCode();
        if (expectedCode == null || !expectedCode.equals(actualCode)) {
            return Result.fail("验证码错误！");
        }

        // 4. find a user using the phone number
        User user = query().eq("phone", loginForm.getPhone()).one();

        // 5. if it's a new user, then we create a user
        if (user == null) {
            user = createUserWithPhone(phone);
        }

        // 6. generate token
        String token = UUID.randomUUID().toString();

        // 7. Use token as key to store user in hash format
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((name, value) -> value.toString()));
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        // 8. Set TTL (expiration time) of user info to be
        stringRedisTemplate.expire(LOGIN_USER_KEY + token,
                Duration.ofSeconds(LOGIN_USER_TTL));
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User newUser = new User();
        newUser.setPhone(phone);
        newUser.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10));
        save(newUser);
        return newUser;
    }
}
