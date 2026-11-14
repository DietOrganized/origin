package com.example.calorie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.calorie.common.BusinessException;
import com.example.calorie.common.ResultCode;
import com.example.calorie.entity.DailySummary;
import com.example.calorie.entity.MealRecord;
import com.example.calorie.mapper.DailySummaryMapper;
import com.example.calorie.mapper.MealRecordMapper;
import com.example.calorie.service.ProfileService;
import com.example.calorie.service.StatsService;
import com.example.calorie.vo.DailyPointVO;
import com.example.calorie.vo.MealTypeDistributionVO;
import com.example.calorie.vo.PeriodCompareVO;
import com.example.calorie.vo.RangeVO;
import com.example.calorie.vo.StatsSummaryVO;
import com.example.calorie.vo.StatsVO;
import com.example.calorie.vo.TodayOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final DailySummaryMapper dailySummaryMapper;
    private final MealRecordMapper mealRecordMapper;
    private final ProfileService profileService;

    @Override
    public TodayOverviewVO today(Long userId) {
        LocalDate today = LocalDate.now();
        Integer target = profileService.getDailyTarget(userId);
        DailySummary summary = find(userId, today);

        TodayOverviewVO vo = new TodayOverviewVO();
        vo.setDate(today);
        vo.setTargetCalories(target);

        BigDecimal calories = summary == null ? BigDecimal.ZERO : nvl(summary.getTotalCalories());
        vo.setTotalCalories(calories);
        vo.setTotalProtein(summary == null ? BigDecimal.ZERO : nvl(summary.getTotalProtein()));
        vo.setTotalFat(summary == null ? BigDecimal.ZERO : nvl(summary.getTotalFat()));
        vo.setTotalCarbs(summary == null ? BigDecimal.ZERO : nvl(summary.getTotalCarbs()));
        vo.setMealCount(summary == null ? 0 : summary.getMealCount());

        if (target != null) {
            vo.setRemainingCalories(BigDecimal.valueOf(target).subtract(calories)
                    .setScale(1, RoundingMode.HALF_UP));
        }
        vo.setAchieved(target != null && target > 0
                && Math.abs(calories.doubleValue() - target) <= target * 0.1);
        vo.setMealTypeDistribution(distribution(userId, today, today, calories));
        return vo;
    }

    @Override
    public StatsVO weekly(Long userId, LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return build(userId, monday, monday.plusDays(6));
    }

    @Override
    public StatsVO monthly(Long userId, YearMonth month) {
        return build(userId, month.atDay(1), month.atEndOfMonth());
    }

    @Override
    public List<DailyPointVO> trend(Long userId, LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "日期区间不合法");
        }
        if (ChronoUnit.DAYS.between(start, end) > 366) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "查询区间最长 366 天");
        }
        return points(userId, start, end);
    }

    private StatsVO build(Long userId, LocalDate start, LocalDate end) {
        List<DailyPointVO> daily = points(userId, start, end);
        Integer target = profileService.getDailyTarget(userId);

        StatsVO vo = new StatsVO();
        vo.setRange(new RangeVO(start, end));
        vo.setDaily(daily);
        vo.setSummary(summarize(daily, target));

        BigDecimal totalCalories = daily.stream()
                .map(DailyPointVO::getCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setMealTypeDistribution(distribution(userId, start, end, totalCalories));
        vo.setCompare(compare(userId, start, end, vo.getSummary()));
        return vo;
    }

    private List<DailyPointVO> points(Long userId, LocalDate start, LocalDate end) {
        Map<LocalDate, DailySummary> map = load(userId, start, end);
        Integer target = profileService.getDailyTarget(userId);

        List<DailyPointVO> list = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            DailySummary summary = map.get(date);
            DailyPointVO point = new DailyPointVO();
            point.setDate(date);
            point.setTargetCalories(target);
            if (summary != null) {
                point.setCalories(nvl(summary.getTotalCalories()));
                point.setProtein(nvl(summary.getTotalProtein()));
                point.setFat(nvl(summary.getTotalFat()));
                point.setCarbs(nvl(summary.getTotalCarbs()));
                point.setMealCount(summary.getMealCount() == null ? 0 : summary.getMealCount());
                point.setAchieved(summary.getIsAchieved() != null && summary.getIsAchieved() == 1);
            }
            list.add(point);
        }
        return list;
    }

    private StatsSummaryVO summarize(List<DailyPointVO> daily, Integer target) {
        StatsSummaryVO summary = new StatsSummaryVO();
        summary.setTargetCalories(target);
        summary.setTotalDays(daily.size());

        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        BigDecimal min = null;
        int achieved = 0;
        int recorded = 0;

        for (DailyPointVO point : daily) {
            if (point.isAchieved()) {
                achieved++;
            }
            if (point.getMealCount() > 0) {
                recorded++;
                calories = calories.add(point.getCalories());
                protein = protein.add(point.getProtein());
                fat = fat.add(point.getFat());
                carbs = carbs.add(point.getCarbs());
                if (point.getCalories().compareTo(max) > 0) {
                    max = point.getCalories();
                }
                if (min == null || point.getCalories().compareTo(min) < 0) {
                    min = point.getCalories();
                }
            }
        }

        summary.setAchievedDays(achieved);
        summary.setRecordedDays(recorded);
        summary.setCompletionRate(daily.isEmpty() ? 0
                : BigDecimal.valueOf(achieved * 1.0 / daily.size())
                .setScale(3, RoundingMode.HALF_UP).doubleValue());
        summary.setMaxCalories(scale(max));
        summary.setMinCalories(scale(min == null ? BigDecimal.ZERO : min));

        if (recorded > 0) {
            BigDecimal divisor = BigDecimal.valueOf(recorded);
            summary.setAvgCalories(calories.divide(divisor, 1, RoundingMode.HALF_UP));
            summary.setAvgProtein(protein.divide(divisor, 1, RoundingMode.HALF_UP));
            summary.setAvgFat(fat.divide(divisor, 1, RoundingMode.HALF_UP));
            summary.setAvgCarbs(carbs.divide(divisor, 1, RoundingMode.HALF_UP));
        }
        return summary;
    }

    private PeriodCompareVO compare(Long userId, LocalDate start, LocalDate end, StatsSummaryVO current) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate prevEnd = start.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(days - 1);

        Map<LocalDate, DailySummary> map = load(userId, prevStart, prevEnd);
        BigDecimal total = BigDecimal.ZERO;
        int recorded = 0;
        for (DailySummary summary : map.values()) {
            if (summary.getMealCount() != null && summary.getMealCount() > 0) {
                total = total.add(nvl(summary.getTotalCalories()));
                recorded++;
            }
        }

        PeriodCompareVO vo = new PeriodCompareVO();
        BigDecimal lastAvg = recorded == 0 ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(recorded), 1, RoundingMode.HALF_UP);
        vo.setLastPeriodAvgCalories(lastAvg);

        BigDecimal delta = current.getAvgCalories().subtract(lastAvg).setScale(1, RoundingMode.HALF_UP);
        vo.setDelta(delta);
        vo.setDeltaRate(lastAvg.compareTo(BigDecimal.ZERO) == 0 ? 0
                : delta.divide(lastAvg, 3, RoundingMode.HALF_UP).doubleValue());
        return vo;
    }

    private List<MealTypeDistributionVO> distribution(Long userId, LocalDate start, LocalDate end,
                                                      BigDecimal totalCalories) {
        QueryWrapper<MealRecord> wrapper = new QueryWrapper<>();
        wrapper.select("meal_type", "SUM(total_calories) AS calories")
                .eq("user_id", userId)
                .between("stat_date", start, end)
                .groupBy("meal_type");

        List<Map<String, Object>> rows = mealRecordMapper.selectMaps(wrapper);
        Map<String, BigDecimal> caloriesByType = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String type = String.valueOf(value(row, "meal_type"));
            BigDecimal value = toDecimal(value(row, "calories"));
            caloriesByType.put(type, value);
        }

        List<String> order = List.of("BREAKFAST", "LUNCH", "DINNER", "SNACK");
        List<MealTypeDistributionVO> list = new ArrayList<>();
        for (String type : order) {
            BigDecimal value = caloriesByType.getOrDefault(type, BigDecimal.ZERO);
            MealTypeDistributionVO vo = new MealTypeDistributionVO();
            vo.setMealType(type);
            vo.setLabel(label(type));
            vo.setCalories(scale(value));
            if (totalCalories != null && totalCalories.compareTo(BigDecimal.ZERO) > 0) {
                vo.setRatio(value.divide(totalCalories, 3, RoundingMode.HALF_UP).doubleValue());
            }
            list.add(vo);
        }
        return list;
    }

    private Map<LocalDate, DailySummary> load(Long userId, LocalDate start, LocalDate end) {
        List<DailySummary> list = dailySummaryMapper.selectList(new LambdaQueryWrapper<DailySummary>()
                .eq(DailySummary::getUserId, userId)
                .between(DailySummary::getStatDate, start, end));
        return list.stream().collect(Collectors.toMap(DailySummary::getStatDate, Function.identity(),
                (a, b) -> a));
    }

    private DailySummary find(Long userId, LocalDate date) {
        return dailySummaryMapper.selectOne(new LambdaQueryWrapper<DailySummary>()
                .eq(DailySummary::getUserId, userId)
                .eq(DailySummary::getStatDate, date));
    }

    private Object value(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(1, RoundingMode.HALF_UP);
    }

    private String label(String mealType) {
        return switch (mealType == null ? "" : mealType) {
            case "BREAKFAST" -> "早餐";
            case "LUNCH" -> "午餐";
            case "DINNER" -> "晚餐";
            case "SNACK" -> "加餐";
            default -> mealType;
        };
    }
}
