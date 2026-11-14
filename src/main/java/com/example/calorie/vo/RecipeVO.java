package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class RecipeVO {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private Integer servings;
    private Integer cookingTimeMin;
    private String difficulty;

    private List<Map<String, Object>> ingredients;
    private List<String> steps;

    private BigDecimal totalCalories;
    private BigDecimal totalProtein;
    private BigDecimal totalFat;
    private BigDecimal totalCarbs;

    private String tags;
    private Integer isPublic;
    private boolean favorite;
    private LocalDateTime createdAt;
}
