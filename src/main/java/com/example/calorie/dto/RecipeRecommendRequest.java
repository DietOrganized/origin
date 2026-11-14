package com.example.calorie.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RecipeRecommendRequest {

    /** CALORIE：按热量上限；INGREDIENT：按现有食材 */
    @Pattern(regexp = "CALORIE|INGREDIENT", message = "模式只能是 CALORIE 或 INGREDIENT")
    private String mode = "INGREDIENT";

    @Size(max = 10, message = "食材最多 10 种")
    private List<String> ingredients;

    @Min(value = 100, message = "热量上限至少 100 kcal")
    @Max(value = 3000, message = "热量上限最多 3000 kcal")
    private Integer maxCalories;

    private String mealType;

    @Min(value = 1, message = "至少推荐 1 道")
    @Max(value = 3, message = "一次最多推荐 3 道")
    private Integer count = 2;
}
