package com.example.calorie.controller;

import com.example.calorie.common.Result;
import com.example.calorie.context.UserContext;
import com.example.calorie.dto.ProfileUpdateRequest;
import com.example.calorie.service.ProfileService;
import com.example.calorie.vo.ProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "02. 个人资料")
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "查看资料与热量目标")
    @GetMapping
    public Result<ProfileVO> get() {
        return Result.ok(profileService.get(UserContext.requireUserId()));
    }

    @Operation(summary = "修改资料（自动重算 BMR/TDEE/目标）")
    @PutMapping
    public Result<ProfileVO> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.ok(profileService.update(UserContext.requireUserId(), request));
    }
}
