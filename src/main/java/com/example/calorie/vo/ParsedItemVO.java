package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ParsedItemVO {

    private Long foodId;
    private String name;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal grams;

    private BigDecimal calories;
    private BigDecimal protein;
    private BigDecimal fat;
    private BigDecimal carbs;

    private BigDecimal confidence;

    /** CACHE / LLM */
    private String source;
}
