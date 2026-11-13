package com.example.calorie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("food")
public class Food {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String alias;
    private String category;

    @TableField("calories_per_100g")
    private BigDecimal caloriesPer100g;

    @TableField("protein_per_100g")
    private BigDecimal proteinPer100g;

    @TableField("fat_per_100g")
    private BigDecimal fatPer100g;

    @TableField("carbs_per_100g")
    private BigDecimal carbsPer100g;

    @TableField("fiber_per_100g")
    private BigDecimal fiberPer100g;

    @TableField("sodium_per_100g")
    private BigDecimal sodiumPer100g;

    private String defaultUnit;
    private BigDecimal defaultUnitGrams;

    /** SYSTEM / LLM / USER */
    private String source;
    private Integer verified;
    private Long createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
