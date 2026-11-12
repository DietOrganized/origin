package com.example.calorie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置。secret 至少 32 字节（HS256 要求 256 bit）。
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private int expireDays = 30;
    private String header = "Authorization";
    private String prefix = "Bearer ";
}
