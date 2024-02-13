package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.util.Map;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

public class LoginInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. Get token in the header
        String token = request.getHeader("authorization");
        // 2. if token does not exist, return 401
        if (StrUtil.isBlank(token)) {
            response.setStatus(401);
            return false;
        }
        // 3. user token to fetch user info in the redis
        String redisKey = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(redisKey);
        // 4. check if user exist, if not, intercept
        if (userMap.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 5. save userDTO to threadLocal and refresh the redis
        UserHolder.saveUser(userDTO);
        stringRedisTemplate.expire(redisKey, Duration.ofSeconds(LOGIN_USER_TTL));
        return true;
    }
}
