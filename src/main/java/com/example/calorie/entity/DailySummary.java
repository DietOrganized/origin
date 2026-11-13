package com.example.calorie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_summary")
public class DailySummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private LocalDate statDate;

    private BigDecimal totalCalories;
    private BigDecimal totalProtein;
    private BigDecimal totalFat;
    private BigDecimal totalCarbs;

    private Integer mealCount;
    private Integer targetCalories;
    private Integer isAchieved;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
