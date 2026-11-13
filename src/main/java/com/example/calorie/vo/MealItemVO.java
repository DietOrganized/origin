package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MealItemVO {

    private Long id;
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
    private String source;
}
