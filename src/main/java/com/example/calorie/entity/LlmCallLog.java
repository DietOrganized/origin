package com.example.calorie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_call_log")
public class LlmCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String scene;
    private String model;
    private String requestSummary;

    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private Integer success;
    private String errorMsg;

    private LocalDateTime createdAt;
}
