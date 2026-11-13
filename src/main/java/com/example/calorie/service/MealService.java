package com.example.calorie.service;

import com.example.calorie.common.PageResult;
import com.example.calorie.dto.MealSaveRequest;
import com.example.calorie.vo.MealVO;

import java.time.LocalDate;
import java.util.List;

public interface MealService {

    MealVO create(Long userId, MealSaveRequest request);

    MealVO update(Long userId, Long id, MealSaveRequest request);

    void delete(Long userId, Long id);

    MealVO get(Long userId, Long id);

    List<MealVO> listByDate(Long userId, LocalDate date);

    PageResult<MealVO> history(Long userId, long page, long size);

    /** 重算某天汇总（增删改餐次后调用） */
    void recomputeDailySummary(Long userId, LocalDate date);
}
