package com.example.calorie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DeepSeek 接口配置。
 */
@Data
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    private String baseUrl = "https://api.deepseek.com";
    private String apiKey;
    private String model = "deepseek-chat";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 60000;
    /** 每用户每日调用上限 */
    private int dailyLimit = 50;
}
