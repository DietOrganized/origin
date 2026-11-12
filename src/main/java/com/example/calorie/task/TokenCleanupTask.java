package com.example.calorie.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.calorie.entity.TokenBlacklist;
import com.example.calorie.mapper.TokenBlacklistMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 每日清理已过期的 token 黑名单记录，避免表无限增长。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final TokenBlacklistMapper tokenBlacklistMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredTokens() {
        int deleted = tokenBlacklistMapper.delete(
                new LambdaQueryWrapper<TokenBlacklist>().lt(TokenBlacklist::getExpireAt, LocalDateTime.now()));
        log.info("清理过期 token 黑名单 {} 条", deleted);
    }
}
