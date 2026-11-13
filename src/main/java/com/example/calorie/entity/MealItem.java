package com.example.calorie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("meal_item")
public class MealItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long mealRecordId;
    private Long foodId;
    private String foodName;

    private BigDecimal quantity;
    private String unit;
    private BigDecimal grams;

    private BigDecimal calories;
    private BigDecimal protein;
    private BigDecimal fat;
    private BigDecimal carbs;

    private BigDecimal confidence;

    /** LLM / MANUAL */
    private String source;
    private Integer sortOrder;

    private LocalDateTime createdAt;
}
