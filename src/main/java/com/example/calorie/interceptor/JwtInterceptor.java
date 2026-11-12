package com.example.calorie.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.calorie.common.BusinessException;
import com.example.calorie.common.ResultCode;
import com.example.calorie.context.UserContext;
import com.example.calorie.entity.TokenBlacklist;
import com.example.calorie.mapper.TokenBlacklistMapper;
import com.example.calorie.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器：验签 → 校验过期 → 校验黑名单 → 写入 UserContext。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final TokenBlacklistMapper tokenBlacklistMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录凭证无效，请重新登录");
        }

        String jti = claims.getId();
        if (jti == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录凭证无效，请重新登录");
        }

        Long blacklistCount = tokenBlacklistMapper.selectCount(
                new LambdaQueryWrapper<TokenBlacklist>().eq(TokenBlacklist::getJti, jti));
        if (blacklistCount != null && blacklistCount > 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "已退出登录，请重新登录");
        }

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        UserContext.set(new UserContext.LoginUser(userId, username, jti, claims.getExpiration()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
