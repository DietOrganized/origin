package com.example.calorie.service;

import com.example.calorie.vo.DailyPointVO;
import com.example.calorie.vo.StatsVO;
import com.example.calorie.vo.TodayOverviewVO;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface StatsService {

    TodayOverviewVO today(Long userId);

    StatsVO weekly(Long userId, LocalDate date);

    StatsVO monthly(Long userId, YearMonth month);

    List<DailyPointVO> trend(Long userId, LocalDate start, LocalDate end);
}
