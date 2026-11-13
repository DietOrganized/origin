package com.example.calorie.service;

import com.example.calorie.common.PageResult;
import com.example.calorie.dto.FoodParseRequest;
import com.example.calorie.vo.FoodParseResultVO;
import com.example.calorie.vo.FoodVO;

public interface FoodService {

    /** 自然语言解析（食物库优先，未命中再调 LLM） */
    FoodParseResultVO parse(Long userId, FoodParseRequest request);

    PageResult<FoodVO> search(String keyword, long page, long size);

    FoodVO getById(Long id);
}
