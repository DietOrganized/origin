package com.example.calorie.controller;

import com.example.calorie.common.Result;
import com.example.calorie.context.UserContext;
import com.example.calorie.dto.AdviceRequest;
import com.example.calorie.service.AdviceService;
import com.example.calorie.vo.AdviceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "07. 智能建议")
@RestController
@RequestMapping("/api/v1/advice")
@RequiredArgsConstructor
public class AdviceController {

    private final AdviceService adviceService;

    @Operation(summary = "生成周/月饮食建议（规则指标 + AI 组织语言）")
    @PostMapping("/generate")
    public Result<AdviceVO> generate(@Valid @RequestBody AdviceRequest request) {
        return Result.ok(adviceService.generate(UserContext.requireUserId(), request));
    }

    @Operation(summary = "历史建议")
    @GetMapping
    public Result<List<AdviceVO>> list(@RequestParam(required = false) String periodType,
                                       @RequestParam(defaultValue = "5") int limit) {
        return Result.ok(adviceService.list(UserContext.requireUserId(), periodType, limit));
    }
}
