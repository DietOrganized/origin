package com.example.calorie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recipe_favorite")
public class RecipeFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long recipeId;
    private LocalDateTime createdAt;
}
