package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PeriodCompareVO {

    private BigDecimal lastPeriodAvgCalories = BigDecimal.ZERO;
    private BigDecimal delta = BigDecimal.ZERO;
    private double deltaRate;
}
