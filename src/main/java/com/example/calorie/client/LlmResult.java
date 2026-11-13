package com.example.calorie.client;

import lombok.Data;

/**
 * 一次 LLM 调用的结果。
 */
@Data
public class LlmResult {

    private String content;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private long latencyMs;
}
