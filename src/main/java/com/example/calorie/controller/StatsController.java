package com.example.calorie.controller;

import com.example.calorie.common.Result;
import com.example.calorie.context.UserContext;
import com.example.calorie.service.StatsService;
import com.example.calorie.vo.DailyPointVO;
import com.example.calorie.vo.StatsVO;
import com.example.calorie.vo.TodayOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "05. 统计")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "今日概览")
    @GetMapping("/today")
    public Result<TodayOverviewVO> today() {
        return Result.ok(statsService.today(UserContext.requireUserId()));
    }

    @Operation(summary = "周统计（不传 date 默认本周）")
    @GetMapping("/weekly")
    public Result<StatsVO> weekly(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(statsService.weekly(UserContext.requireUserId(),
                date == null ? LocalDate.now() : date));
    }

    @Operation(summary = "月统计（month 格式 yyyy-MM）")
    @GetMapping("/monthly")
    public Result<StatsVO> monthly(@RequestParam(required = false) String month) {
        YearMonth yearMonth = month == null ? YearMonth.now() : YearMonth.parse(month);
        return Result.ok(statsService.monthly(UserContext.requireUserId(), yearMonth));
    }

    @Operation(summary = "自定义区间每日热量趋势")
    @GetMapping("/trend")
    public Result<List<DailyPointVO>> trend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return Result.ok(statsService.trend(UserContext.requireUserId(), start, end));
    }
}
