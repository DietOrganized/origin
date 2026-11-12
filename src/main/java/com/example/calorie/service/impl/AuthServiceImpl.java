package com.example.calorie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.calorie.common.BusinessException;
import com.example.calorie.common.ResultCode;
import com.example.calorie.context.UserContext;
import com.example.calorie.dto.LoginRequest;
import com.example.calorie.dto.RegisterRequest;
import com.example.calorie.entity.TokenBlacklist;
import com.example.calorie.entity.User;
import com.example.calorie.entity.UserProfile;
import com.example.calorie.mapper.TokenBlacklistMapper;
import com.example.calorie.mapper.UserMapper;
import com.example.calorie.mapper.UserProfileMapper;
import com.example.calorie.service.AuthService;
import com.example.calorie.util.JwtUtil;
import com.example.calorie.vo.LoginVO;
import com.example.calorie.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final TokenBlacklistMapper tokenBlacklistMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterRequest request) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已被占用");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname() : request.getUsername());
        user.setStatus(1);
        userMapper.insert(user);

        // 注册时初始化一份空资料，前端进「我的」页不用再判空
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setGender("UNKNOWN");
        profile.setActivityLevel("SEDENTARY");
        profile.setGoalType("MAINTAIN");
        profile.setTargetManual(0);
        userProfileMapper.insert(profile);

        log.info("新用户注册: id={}, username={}", user.getId(), user.getUsername());
        return toVO(user);
    }

    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        User update = new User();
        update.setId(user.getId());
        update.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(update);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setExpireDays(jwtUtil.getExpireDays());
        vo.setExpiresAt(LocalDateTime.now().plusDays(jwtUtil.getExpireDays()));
        vo.setUser(toVO(user));

        log.info("用户登录: id={}, username={}", user.getId(), user.getUsername());
        return vo;
    }

    @Override
    public void logout() {
        UserContext.LoginUser loginUser = UserContext.require();
        TokenBlacklist entity = new TokenBlacklist();
        entity.setJti(loginUser.jti());
        entity.setUserId(loginUser.userId());
        entity.setExpireAt(LocalDateTime.ofInstant(loginUser.expireAt().toInstant(), ZoneId.systemDefault()));
        tokenBlacklistMapper.insert(entity);
        log.info("用户退出登录: id={}, jti={}", loginUser.userId(), loginUser.jti());
    }

    @Override
    public UserVO currentUser() {
        User user = userMapper.selectById(UserContext.requireUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在");
        }
        return toVO(user);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
