package com.example.calorie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "recipe", autoResultMap = true)
public class Recipe {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** NULL 表示公共菜谱 */
    private Long userId;

    private String title;
    private String description;
    private Integer servings;
    private Integer cookingTimeMin;
    private String difficulty;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> ingredients;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> steps;

    private BigDecimal totalCalories;
    private BigDecimal totalProtein;
    private BigDecimal totalFat;
    private BigDecimal totalCarbs;

    private String tags;
    private String sourcePrompt;
    private String model;
    private Integer isPublic;

    private LocalDateTime createdAt;
}
