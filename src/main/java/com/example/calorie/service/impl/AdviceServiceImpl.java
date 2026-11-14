package com.example.calorie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.calorie.client.DeepSeekClient;
import com.example.calorie.client.LlmResult;
import com.example.calorie.common.BusinessException;
import com.example.calorie.common.ResultCode;
import com.example.calorie.dto.AdviceRequest;
import com.example.calorie.entity.DailySummary;
import com.example.calorie.entity.MealRecord;
import com.example.calorie.entity.NutritionAdvice;
import com.example.calorie.mapper.DailySummaryMapper;
import com.example.calorie.mapper.MealRecordMapper;
import com.example.calorie.mapper.NutritionAdviceMapper;
import com.example.calorie.prompt.PromptLoader;
import com.example.calorie.service.AdviceService;
import com.example.calorie.service.ProfileService;
import com.example.calorie.vo.AdviceVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdviceServiceImpl implements AdviceService {

    private static final String SYSTEM_PROMPT = "你是一名温和、专业的中国营养师，只根据给定的统计数据给出建议，绝不编造数据。";

    private final NutritionAdviceMapper nutritionAdviceMapper;
    private final DailySummaryMapper dailySummaryMapper;
    private final MealRecordMapper mealRecordMapper;
    private final ProfileService profileService;
    private final DeepSeekClient deepSeekClient;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdviceVO generate(Long userId, AdviceRequest request) {
        String periodType = StringUtils.hasText(request.getPeriodType()) ? request.getPeriodType() : "WEEK";
        LocalDate[] range = resolveRange(periodType, request.getPeriodStart());

        List<DailySummary> summaries = dailySummaryMapper.selectList(new LambdaQueryWrapper<DailySummary>()
                .eq(DailySummary::getUserId, userId)
                .between(DailySummary::getStatDate, range[0], range[1]));

        List<DailySummary> recorded = summaries.stream()
                .filter(s -> s.getMealCount() != null && s.getMealCount() > 0)
                .toList();
        if (recorded.size() < 3) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "该周期只有 " + recorded.size() + " 天记录，至少需要 3 天才能生成建议");
        }

        Integer target = profileService.getDailyTarget(userId);
        Map<String, Object> metrics = buildMetrics(recorded, target, range,
                distribution(userId, range[0], range[1]));

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("period", "WEEK".equals(periodType) ? "本周" : "本月");
        variables.put("goal", goalText(userId));
        try {
            variables.put("metrics", objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(metrics));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "统计数据序列化失败");
        }

        String userPrompt = promptLoader.render("advice", variables);
        LlmResult result = deepSeekClient.chat("ADVICE", userId, SYSTEM_PROMPT, userPrompt, 0.6, false);

        NutritionAdvice advice = new NutritionAdvice();
        advice.setUserId(userId);
        advice.setPeriodType(periodType);
        advice.setPeriodStart(range[0]);
        advice.setPeriodEnd(range[1]);
        advice.setContent(result.getContent());
        advice.setMetrics(metrics);
        advice.setModel(result.getModel());
        nutritionAdviceMapper.insert(advice);

        return toVO(advice);
    }

    @Override
    public List<AdviceVO> list(Long userId, String periodType, int limit) {
        LambdaQueryWrapper<NutritionAdvice> wrapper = new LambdaQueryWrapper<NutritionAdvice>()
                .eq(NutritionAdvice::getUserId, userId)
                .eq(StringUtils.hasText(periodType), NutritionAdvice::getPeriodType, periodType)
                .orderByDesc(NutritionAdvice::getId)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 20));
        return nutritionAdviceMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    private LocalDate[] resolveRange(String periodType, LocalDate input) {
        LocalDate base = input == null ? LocalDate.now() : input;
        if ("MONTH".equals(periodType)) {
            YearMonth month = YearMonth.from(base);
            return new LocalDate[]{month.atDay(1), month.atEndOfMonth()};
        }
        LocalDate monday = base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new LocalDate[]{monday, monday.plusDays(6)};
    }

    private Map<String, Object> buildMetrics(List<DailySummary> recorded, Integer target,
                                             LocalDate[] range, Map<String, Object> distribution) {
        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        int achieved = 0;
        int over = 0;
        int under = 0;
        BigDecimal weekendSum = BigDecimal.ZERO;
        int weekendDays = 0;
        BigDecimal weekdaySum = BigDecimal.ZERO;
        int weekdayDays = 0;
        BigDecimal max = BigDecimal.ZERO;

        for (DailySummary item : recorded) {
            BigDecimal value = nvl(item.getTotalCalories());
            calories = calories.add(value);
            protein = protein.add(nvl(item.getTotalProtein()));
            fat = fat.add(nvl(item.getTotalFat()));
            carbs = carbs.add(nvl(item.getTotalCarbs()));
            if (item.getIsAchieved() != null && item.getIsAchieved() == 1) {
                achieved++;
            }
            if (target != null && target > 0) {
                if (value.doubleValue() > target * 1.1) {
                    over++;
                } else if (value.doubleValue() < target * 0.9) {
                    under++;
                }
            }
            DayOfWeek day = item.getStatDate().getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                weekendSum = weekendSum.add(value);
                weekendDays++;
            } else {
                weekdaySum = weekdaySum.add(value);
                weekdayDays++;
            }
            if (value.compareTo(max) > 0) {
                max = value;
            }
        }

        int days = recorded.size();
        BigDecimal divisor = BigDecimal.valueOf(days);
        BigDecimal avgCalories = calories.divide(divisor, 1, RoundingMode.HALF_UP);
        BigDecimal avgProtein = protein.divide(divisor, 1, RoundingMode.HALF_UP);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("periodStart", range[0].toString());
        metrics.put("periodEnd", range[1].toString());
        metrics.put("totalDays", range[0].until(range[1]).getDays() + 1);
        metrics.put("recordedDays", days);
        metrics.put("targetCalories", target);
        metrics.put("avgCalories", avgCalories);
        metrics.put("maxCalories", max.setScale(1, RoundingMode.HALF_UP));
        metrics.put("avgProtein", avgProtein);
        metrics.put("avgFat", fat.divide(divisor, 1, RoundingMode.HALF_UP));
        metrics.put("avgCarbs", carbs.divide(divisor, 1, RoundingMode.HALF_UP));
        metrics.put("achievedDays", achieved);
        metrics.put("overDays", over);
        metrics.put("underDays", under);
        metrics.put("achievementRate", BigDecimal.valueOf(achieved * 1.0 / days)
                .setScale(2, RoundingMode.HALF_UP).doubleValue());
        metrics.put("proteinEnergyRatio", calories.compareTo(BigDecimal.ZERO) == 0 ? 0
                : protein.multiply(BigDecimal.valueOf(4))
                .divide(calories, 3, RoundingMode.HALF_UP).doubleValue());
        metrics.put("weekendAvgCalories", weekendDays == 0 ? null
                : weekendSum.divide(BigDecimal.valueOf(weekendDays), 1, RoundingMode.HALF_UP));
        metrics.put("weekdayAvgCalories", weekdayDays == 0 ? null
                : weekdaySum.divide(BigDecimal.valueOf(weekdayDays), 1, RoundingMode.HALF_UP));
        metrics.put("mealDistribution", distribution);
        return metrics;
    }

    private Map<String, Object> distribution(Long userId, LocalDate start, LocalDate end) {
        QueryWrapper<MealRecord> wrapper = new QueryWrapper<>();
        wrapper.select("meal_type", "SUM(total_calories) AS calories")
                .eq("user_id", userId)
                .between("stat_date", start, end)
                .groupBy("meal_type");
        List<Map<String, Object>> rows = mealRecordMapper.selectMaps(wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object type = row.containsKey("meal_type") ? row.get("meal_type") : row.get("MEAL_TYPE");
            Object calories = row.containsKey("calories") ? row.get("calories") : row.get("CALORIES");
            result.put(String.valueOf(type), calories);
        }
        return result;
    }

    private String goalText(Long userId) {
        Integer target = profileService.getDailyTarget(userId);
        return target == null ? "未设置目标" : "每日热量目标 " + target + " kcal";
    }

    private AdviceVO toVO(NutritionAdvice advice) {
        AdviceVO vo = new AdviceVO();
        vo.setId(advice.getId());
        vo.setPeriodType(advice.getPeriodType());
        vo.setPeriodStart(advice.getPeriodStart());
        vo.setPeriodEnd(advice.getPeriodEnd());
        vo.setContent(advice.getContent());
        vo.setMetrics(advice.getMetrics());
        vo.setModel(advice.getModel());
        vo.setCreatedAt(advice.getCreatedAt());
        return vo;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
