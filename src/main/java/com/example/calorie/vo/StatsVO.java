package com.example.calorie.vo;

import lombok.Data;

import java.util.List;

@Data
public class StatsVO {

    private RangeVO range;
    private List<DailyPointVO> daily;
    private StatsSummaryVO summary;
    private List<MealTypeDistributionVO> mealTypeDistribution;
    private PeriodCompareVO compare;
}
