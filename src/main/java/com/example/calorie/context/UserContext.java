package com.example.calorie.context;

import com.example.calorie.common.BusinessException;
import com.example.calorie.common.ResultCode;

import java.util.Date;

/**
 * 当前登录用户上下文（ThreadLocal）。由 JwtInterceptor 写入，请求结束清理。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public record LoginUser(Long userId, String username, String jti, Date expireAt) {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long requireUserId() {
        return require().userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
