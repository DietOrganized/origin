package com.example.calorie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.calorie.dto.ProfileUpdateRequest;
import com.example.calorie.entity.UserProfile;
import com.example.calorie.mapper.UserProfileMapper;
import com.example.calorie.service.ProfileService;
import com.example.calorie.util.CalorieCalculator;
import com.example.calorie.vo.ProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileMapper userProfileMapper;

    @Override
    public ProfileVO get(Long userId) {
        return toVO(getOrCreate(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProfileVO update(Long userId, ProfileUpdateRequest request) {
        UserProfile profile = getOrCreate(userId);

        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getBirthDate() != null) {
            profile.setBirthDate(request.getBirthDate());
        }
        if (request.getHeightCm() != null) {
            profile.setHeightCm(request.getHeightCm());
        }
        if (request.getWeightKg() != null) {
            profile.setWeightKg(request.getWeightKg());
        }
        if (request.getActivityLevel() != null) {
            profile.setActivityLevel(request.getActivityLevel());
        }
        if (request.getGoalType() != null) {
            profile.setGoalType(request.getGoalType());
        }
        if (request.getTargetWeightKg() != null) {
            profile.setTargetWeightKg(request.getTargetWeightKg());
        }
        if (request.getTargetManual() != null) {
            profile.setTargetManual(request.getTargetManual());
        }
        if (request.getDailyCalorieTarget() != null) {
            profile.setDailyCalorieTarget(request.getDailyCalorieTarget());
        }

        int age = CalorieCalculator.age(profile.getBirthDate(), LocalDate.now());
        Integer bmr = CalorieCalculator.bmr(profile.getGender(), profile.getHeightCm(),
                profile.getWeightKg(), age);
        Integer tdee = CalorieCalculator.tdee(bmr, profile.getActivityLevel());

        profile.setBmr(bmr);
        profile.setTdee(tdee);

        boolean manual = profile.getTargetManual() != null && profile.getTargetManual() == 1
                && profile.getDailyCalorieTarget() != null;
        if (!manual) {
            profile.setTargetManual(0);
            profile.setDailyCalorieTarget(CalorieCalculator.target(tdee, bmr, profile.getGoalType()));
        }

        userProfileMapper.updateById(profile);
        return toVO(profile);
    }

    @Override
    public Integer getDailyTarget(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        return profile == null ? null : profile.getDailyCalorieTarget();
    }

    private UserProfile getOrCreate(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        if (profile != null) {
            return profile;
        }
        profile = new UserProfile();
        profile.setUserId(userId);
        profile.setGender("UNKNOWN");
        profile.setActivityLevel("SEDENTARY");
        profile.setGoalType("MAINTAIN");
        profile.setTargetManual(0);
        userProfileMapper.insert(profile);
        return profile;
    }

    private ProfileVO toVO(UserProfile profile) {
        ProfileVO vo = new ProfileVO();
        vo.setUserId(profile.getUserId());
        vo.setGender(profile.getGender());
        vo.setBirthDate(profile.getBirthDate());
        vo.setAge(profile.getBirthDate() == null ? null
                : CalorieCalculator.age(profile.getBirthDate(), LocalDate.now()));
        vo.setHeightCm(profile.getHeightCm());
        vo.setWeightKg(profile.getWeightKg());
        vo.setActivityLevel(profile.getActivityLevel());
        vo.setGoalType(profile.getGoalType());
        vo.setTargetWeightKg(profile.getTargetWeightKg());
        vo.setBmr(profile.getBmr());
        vo.setTdee(profile.getTdee());
        vo.setDailyCalorieTarget(profile.getDailyCalorieTarget());
        vo.setTargetManual(profile.getTargetManual());
        return vo;
    }
}
