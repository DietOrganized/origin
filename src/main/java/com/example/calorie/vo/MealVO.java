package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MealVO {

    private Long id;
    private String mealType;
    private String mealTypeLabel;
    private LocalDateTime eatenAt;
    private LocalDate statDate;

    private BigDecimal totalCalories;
    private BigDecimal totalProtein;
    private BigDecimal totalFat;
    private BigDecimal totalCarbs;

    private String rawInput;
    private String note;

    private List<MealItemVO> items;
}
