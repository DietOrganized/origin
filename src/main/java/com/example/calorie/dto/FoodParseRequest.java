package com.example.calorie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FoodParseRequest {

    @NotBlank(message = "请输入吃了什么")
    @Size(max = 200, message = "描述最长 200 个字符")
    private String text;

    /** 可选：BREAKFAST / LUNCH / DINNER / SNACK */
    private String mealType;
}
