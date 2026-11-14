package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyPointVO {

    private LocalDate date;
    private BigDecimal calories = BigDecimal.ZERO;
    private BigDecimal protein = BigDecimal.ZERO;
    private BigDecimal fat = BigDecimal.ZERO;
    private BigDecimal carbs = BigDecimal.ZERO;
    private Integer targetCalories;
    private boolean achieved;
    private int mealCount;
}
