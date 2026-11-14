package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TodayOverviewVO {

    private LocalDate date;
    private Integer targetCalories;
    private BigDecimal totalCalories = BigDecimal.ZERO;
    private BigDecimal remainingCalories = BigDecimal.ZERO;
    private BigDecimal totalProtein = BigDecimal.ZERO;
    private BigDecimal totalFat = BigDecimal.ZERO;
    private BigDecimal totalCarbs = BigDecimal.ZERO;
    private int mealCount;
    private boolean achieved;
    private List<MealTypeDistributionVO> mealTypeDistribution;
}
