package com.example.calorie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户身体数据与热量目标。
 */
@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** MALE / FEMALE / UNKNOWN */
    private String gender;

    private LocalDate birthDate;

    private BigDecimal heightCm;

    private BigDecimal weightKg;

    /** SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE */
    private String activityLevel;

    /** LOSE_FAT / MAINTAIN / GAIN_MUSCLE */
    private String goalType;

    private BigDecimal targetWeightKg;

    /** 基础代谢（计算值） */
    private Integer bmr;

    /** 每日总消耗（计算值） */
    private Integer tdee;

    private Integer dailyCalorieTarget;

    /** 1 表示用户手动指定目标 */
    private Integer targetManual;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
