package com.example.calorie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "nutrition_advice", autoResultMap = true)
public class NutritionAdvice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metrics;

    private String model;
    private LocalDateTime createdAt;
}
