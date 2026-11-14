package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StatsSummaryVO {

    private BigDecimal avgCalories = BigDecimal.ZERO;
    private BigDecimal maxCalories = BigDecimal.ZERO;
    private BigDecimal minCalories = BigDecimal.ZERO;
    private Integer targetCalories;
    private int achievedDays;
    private int totalDays;
    private int recordedDays;
    private double completionRate;
    private BigDecimal avgProtein = BigDecimal.ZERO;
    private BigDecimal avgFat = BigDecimal.ZERO;
    private BigDecimal avgCarbs = BigDecimal.ZERO;
}
