package com.example.calorie.controller;

import com.example.calorie.common.PageResult;
import com.example.calorie.common.Result;
import com.example.calorie.context.UserContext;
import com.example.calorie.dto.MealSaveRequest;
import com.example.calorie.service.MealService;
import com.example.calorie.vo.MealVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "04. 餐次记录")
@RestController
@RequestMapping("/api/v1/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @Operation(summary = "新增一餐（可直接传自然语言 rawInput）")
    @PostMapping
    public Result<MealVO> create(@Valid @RequestBody MealSaveRequest request) {
        return Result.ok(mealService.create(UserContext.requireUserId(), request));
    }

    @Operation(summary = "按日期查询当天全部餐次")
    @GetMapping
    public Result<List<MealVO>> listByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(mealService.listByDate(UserContext.requireUserId(), date));
    }

    @Operation(summary = "历史记录分页")
    @GetMapping("/history")
    public Result<PageResult<MealVO>> history(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        return Result.ok(mealService.history(UserContext.requireUserId(), page, size));
    }

    @Operation(summary = "餐次详情")
    @GetMapping("/{id}")
    public Result<MealVO> detail(@PathVariable Long id) {
        return Result.ok(mealService.get(UserContext.requireUserId(), id));
    }

    @Operation(summary = "修改餐次")
    @PutMapping("/{id}")
    public Result<MealVO> update(@PathVariable Long id, @Valid @RequestBody MealSaveRequest request) {
        return Result.ok(mealService.update(UserContext.requireUserId(), id, request));
    }

    @Operation(summary = "删除餐次（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mealService.delete(UserContext.requireUserId(), id);
        return Result.ok();
    }
}
