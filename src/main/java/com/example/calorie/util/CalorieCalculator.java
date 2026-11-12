package com.example.calorie.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * 热量计算：Mifflin-St Jeor 公式 + 活动系数 + 目标调整。
 * 公式写在这里，答辩时可直接指着讲。
 */
public final class CalorieCalculator {

    private CalorieCalculator() {
    }

    public static int age(LocalDate birthDate, LocalDate today) {
        if (birthDate == null || birthDate.isAfter(today)) {
            return 0;
        }
        return Period.between(birthDate, today).getYears();
    }

    /**
     * 基础代谢率（kcal/天）。数据不全返回 null。
     */
    public static Integer bmr(String gender, BigDecimal heightCm, BigDecimal weightKg, int age) {
        if (heightCm == null || weightKg == null || age <= 0) {
            return null;
        }
        double base = 10 * weightKg.doubleValue() + 6.25 * heightCm.doubleValue() - 5 * age;
        double value = "FEMALE".equalsIgnoreCase(gender) ? base - 161 : base + 5;
        return (int) Math.round(value);
    }

    public static double activityFactor(String activityLevel) {
        if (activityLevel == null) {
            return 1.2;
        }
        return switch (activityLevel.toUpperCase()) {
            case "LIGHT" -> 1.375;
            case "MODERATE" -> 1.55;
            case "ACTIVE" -> 1.725;
            case "VERY_ACTIVE" -> 1.9;
            default -> 1.2;
        };
    }

    public static Integer tdee(Integer bmr, String activityLevel) {
        if (bmr == null) {
            return null;
        }
        return (int) Math.round(bmr * activityFactor(activityLevel));
    }

    /**
     * 按目标给每日热量目标：减脂 -15%，维持不变，增肌 +10%。
     * 减脂目标不低于 BMR*1.1，且不低于 1200 kcal。
     */
    public static Integer target(Integer tdee, Integer bmr, String goalType) {
        if (tdee == null) {
            return null;
        }
        int value = switch (goalType == null ? "MAINTAIN" : goalType.toUpperCase()) {
            case "LOSE_FAT" -> (int) Math.round(tdee * 0.85);
            case "GAIN_MUSCLE" -> (int) Math.round(tdee * 1.10);
            default -> tdee;
        };
        if ("LOSE_FAT".equalsIgnoreCase(goalType) && bmr != null) {
            value = Math.max(value, (int) Math.round(bmr * 1.1));
            value = Math.max(value, 1200);
        }
        return value;
    }
}
