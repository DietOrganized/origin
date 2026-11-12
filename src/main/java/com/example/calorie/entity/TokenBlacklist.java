package com.example.calorie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登出后的 token 黑名单，配合 30 天免登录。
 */
@Data
@TableName("token_blacklist")
public class TokenBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** JWT 唯一标识 */
    private String jti;

    private Long userId;

    /** token 原始过期时间，到期后可清理 */
    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
}
