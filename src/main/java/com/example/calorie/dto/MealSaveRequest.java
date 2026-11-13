package com.example.calorie.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MealSaveRequest {

    @NotBlank(message = "餐次类型不能为空")
    @Pattern(regexp = "BREAKFAST|LUNCH|DINNER|SNACK", message = "餐次类型只能是 BREAKFAST/LUNCH/DINNER/SNACK")
    private String mealType;

    /** 不传默认当前时间 */
    private LocalDateTime eatenAt;

    /** 自然语言描述，和 items 二选一 */
    @Size(max = 500, message = "描述最长 500 个字符")
    private String rawInput;

    @Size(max = 255, message = "备注最长 255 个字符")
    private String note;

    @Valid
    private List<MealItemRequest> items;
}
