package com.example.calorie.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {

    @Pattern(regexp = "MALE|FEMALE|UNKNOWN", message = "性别只能是 MALE/FEMALE/UNKNOWN")
    private String gender;

    private LocalDate birthDate;

    @DecimalMin(value = "50.0", message = "身高需在 50-250cm 之间")
    @DecimalMax(value = "250.0", message = "身高需在 50-250cm 之间")
    private BigDecimal heightCm;

    @DecimalMin(value = "20.0", message = "体重需在 20-300kg 之间")
    @DecimalMax(value = "300.0", message = "体重需在 20-300kg 之间")
    private BigDecimal weightKg;

    @Pattern(regexp = "SEDENTARY|LIGHT|MODERATE|ACTIVE|VERY_ACTIVE", message = "活动水平不合法")
    private String activityLevel;

    @Pattern(regexp = "LOSE_FAT|MAINTAIN|GAIN_MUSCLE", message = "目标类型不合法")
    private String goalType;

    @DecimalMin(value = "20.0", message = "目标体重需在 20-300kg 之间")
    @DecimalMax(value = "300.0", message = "目标体重需在 20-300kg 之间")
    private BigDecimal targetWeightKg;

    @Min(value = 800, message = "每日热量目标需在 800-6000 kcal 之间")
    @Max(value = 6000, message = "每日热量目标需在 800-6000 kcal 之间")
    private Integer dailyCalorieTarget;

    /** 1 表示用户手动指定目标，0 表示按公式计算 */
    @Min(0)
    @Max(1)
    private Integer targetManual;
}
