package com.example.calorie.service;

import com.example.calorie.dto.AdviceRequest;
import com.example.calorie.vo.AdviceVO;

import java.util.List;

public interface AdviceService {

    AdviceVO generate(Long userId, AdviceRequest request);

    List<AdviceVO> list(Long userId, String periodType, int limit);
}
