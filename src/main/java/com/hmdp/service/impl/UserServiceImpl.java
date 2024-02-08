package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

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

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. verify the phone number
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }
        // 2. generate verification code
        String code = RandomUtil.randomNumbers(6);
        // 3. store the verification code in the session
        session.setAttribute("code", code);
        // 4. send code to phone number
        log.info("Send Verification code: " + code + " to phone number: " + phone);
        // 5. return ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        // 1. verify the phone number
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }

        // 2. verify the code
        Object expectedCode = session.getAttribute("code");
        String actualCode = loginForm.getCode();
        if (expectedCode == null || !expectedCode.toString().equals(actualCode)) {
            return Result.fail("验证码错误！");
        }

        // 3. find a user using the phone number
        User user = query().eq("phone", loginForm.getPhone()).one();

        // 4. if it's a new user, then we create a user
        if (user == null) {
            user = createUserWithPhone(phone);
        }

        // 5. store the user in the session
        session.setAttribute("user", user);
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User newUser = new User();
        newUser.setPhone(phone);
        newUser.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10));
        save(newUser);
        return newUser;
    }
}
