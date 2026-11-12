package com.example.calorie.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginVO {

    private String token;

    private String tokenType = "Bearer";

    /** token 过期时间 */
    private LocalDateTime expiresAt;

    /** 有效天数，前端可据此提示「30 天内免登录」 */
    private Integer expireDays;

    private UserVO user;
}
