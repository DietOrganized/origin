package com.example.calorie.service;

import com.example.calorie.dto.ProfileUpdateRequest;
import com.example.calorie.vo.ProfileVO;

public interface ProfileService {

    ProfileVO get(Long userId);

    ProfileVO update(Long userId, ProfileUpdateRequest request);

    /** 取当前用户每日热量目标，没有则返回 null */
    Integer getDailyTarget(Long userId);
}
