package com.example.calorie.service;

import com.example.calorie.common.PageResult;
import com.example.calorie.dto.RecipeRecommendRequest;
import com.example.calorie.vo.RecipeVO;

import java.util.List;

public interface RecipeService {

    List<RecipeVO> recommend(Long userId, RecipeRecommendRequest request);

    PageResult<RecipeVO> list(Long userId, long page, long size, boolean favoriteOnly);

    RecipeVO get(Long userId, Long id);

    void favorite(Long userId, Long id);

    void unfavorite(Long userId, Long id);

    void delete(Long userId, Long id);
}
