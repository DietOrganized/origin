package com.example.calorie.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FoodParseResultVO {

    private List<ParsedItemVO> items;

    private BigDecimal totalCalories;
    private BigDecimal totalProtein;
    private BigDecimal totalFat;
    private BigDecimal totalCarbs;

    private String note;

    /** true 表示命中了本地食物库，未调用大模型 */
    private boolean fromCache;
}
