package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FoodVO {

    private Long id;
    private String name;
    private String alias;
    private String category;

    private BigDecimal caloriesPer100g;
    private BigDecimal proteinPer100g;
    private BigDecimal fatPer100g;
    private BigDecimal carbsPer100g;

    private String defaultUnit;
    private BigDecimal defaultUnitGrams;

    private String source;
    private Integer verified;
}
