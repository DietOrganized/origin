package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProfileVO {

    private Long userId;
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String activityLevel;
    private String goalType;
    private BigDecimal targetWeightKg;

    private Integer bmr;
    private Integer tdee;
    private Integer dailyCalorieTarget;
    private Integer targetManual;
}
