package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MealTypeDistributionVO {

    private String mealType;
    private String label;
    private BigDecimal calories;
    private double ratio;
}
