package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. get user in the session
        Object user = request.getSession().getAttribute("user");
        // 2. check if user exist
        if (user == null) {
            // 3. if it does not exist, intercept
            response.setStatus(401);
            return false;
        }
        // 4. if the user exist, save the user to our Thread Local
        UserHolder.saveUser((User) user);
        return true;
    }



}
