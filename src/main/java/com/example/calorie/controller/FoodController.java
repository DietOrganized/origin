package com.example.calorie.controller;

import com.example.calorie.common.PageResult;
import com.example.calorie.common.Result;
import com.example.calorie.context.UserContext;
import com.example.calorie.dto.FoodParseRequest;
import com.example.calorie.service.FoodService;
import com.example.calorie.vo.FoodParseResultVO;
import com.example.calorie.vo.FoodVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "03. 食物解析")
@RestController
@RequestMapping("/api/v1/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @Operation(summary = "自然语言解析食物热量（不落库，供确认后记账）")
    @PostMapping("/parse")
    public Result<FoodParseResultVO> parse(@Valid @RequestBody FoodParseRequest request) {
        return Result.ok(foodService.parse(UserContext.requireUserId(), request));
    }

    @Operation(summary = "搜索食物库")
    @GetMapping("/search")
    public Result<PageResult<FoodVO>> search(@RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(foodService.search(keyword, page, size));
    }

    @Operation(summary = "食物详情")
    @GetMapping("/{id}")
    public Result<FoodVO> detail(@PathVariable Long id) {
        return Result.ok(foodService.getById(id));
    }
}
