package com.example.calorie.controller;

import com.example.calorie.common.PageResult;
import com.example.calorie.common.Result;
import com.example.calorie.context.UserContext;
import com.example.calorie.dto.RecipeRecommendRequest;
import com.example.calorie.service.RecipeService;
import com.example.calorie.vo.RecipeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "06. 菜谱推荐")
@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @Operation(summary = "AI 推荐菜谱（按食材或热量上限）")
    @PostMapping("/recommend")
    public Result<List<RecipeVO>> recommend(@Valid @RequestBody RecipeRecommendRequest request) {
        return Result.ok(recipeService.recommend(UserContext.requireUserId(), request));
    }

    @Operation(summary = "菜谱列表（我的 + 公共）")
    @GetMapping
    public Result<PageResult<RecipeVO>> list(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(defaultValue = "false") boolean favorite) {
        return Result.ok(recipeService.list(UserContext.requireUserId(), page, size, favorite));
    }

    @Operation(summary = "菜谱详情")
    @GetMapping("/{id}")
    public Result<RecipeVO> detail(@PathVariable Long id) {
        return Result.ok(recipeService.get(UserContext.requireUserId(), id));
    }

    @Operation(summary = "收藏菜谱")
    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id) {
        recipeService.favorite(UserContext.requireUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id) {
        recipeService.unfavorite(UserContext.requireUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "删除自己的菜谱")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recipeService.delete(UserContext.requireUserId(), id);
        return Result.ok();
    }
}
