package com.example.calorie.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MealItemRequest {

    private Long foodId;

    @NotBlank(message = "食物名称不能为空")
    @Size(max = 100, message = "食物名称过长")
    private String foodName;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.01", message = "数量必须大于 0")
    private BigDecimal quantity;

    @Size(max = 20, message = "单位过长")
    private String unit;

    @NotNull(message = "克重不能为空")
    @DecimalMin(value = "0.1", message = "克重必须大于 0")
    private BigDecimal grams;

    @NotNull(message = "热量不能为空")
    @DecimalMin(value = "0", message = "热量不能为负")
    private BigDecimal calories;

    @DecimalMin(value = "0", message = "蛋白质不能为负")
    private BigDecimal protein;

    @DecimalMin(value = "0", message = "脂肪不能为负")
    private BigDecimal fat;

    @DecimalMin(value = "0", message = "碳水不能为负")
    private BigDecimal carbs;

    private BigDecimal confidence;

    /** LLM / MANUAL，默认 MANUAL */
    private String source;
}
