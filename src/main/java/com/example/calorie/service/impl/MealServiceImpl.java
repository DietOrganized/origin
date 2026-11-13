package com.example.calorie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.calorie.common.BusinessException;
import com.example.calorie.common.PageResult;
import com.example.calorie.common.ResultCode;
import com.example.calorie.dto.FoodParseRequest;
import com.example.calorie.dto.MealItemRequest;
import com.example.calorie.dto.MealSaveRequest;
import com.example.calorie.entity.DailySummary;
import com.example.calorie.entity.MealItem;
import com.example.calorie.entity.MealRecord;
import com.example.calorie.mapper.DailySummaryMapper;
import com.example.calorie.mapper.MealItemMapper;
import com.example.calorie.mapper.MealRecordMapper;
import com.example.calorie.service.FoodService;
import com.example.calorie.service.MealService;
import com.example.calorie.service.ProfileService;
import com.example.calorie.vo.FoodParseResultVO;
import com.example.calorie.vo.MealItemVO;
import com.example.calorie.vo.MealVO;
import com.example.calorie.vo.ParsedItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final MealRecordMapper mealRecordMapper;
    private final MealItemMapper mealItemMapper;
    private final DailySummaryMapper dailySummaryMapper;
    private final ProfileService profileService;
    private final FoodService foodService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MealVO create(Long userId, MealSaveRequest request) {
        LocalDateTime eatenAt = request.getEatenAt() == null ? LocalDateTime.now() : request.getEatenAt();
        validateEatenAt(eatenAt);

        List<MealItem> items = buildItems(userId, request);
        if (items.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请至少提供一项食物，或填写自然语言描述");
        }

        MealRecord record = new MealRecord();
        record.setUserId(userId);
        record.setMealType(request.getMealType());
        record.setEatenAt(eatenAt);
        record.setStatDate(eatenAt.toLocalDate());
        record.setRawInput(request.getRawInput());
        record.setNote(request.getNote());
        fillTotals(record, items);
        mealRecordMapper.insert(record);

        saveItems(record.getId(), items);
        recomputeDailySummary(userId, record.getStatDate());
        return toVO(record, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MealVO update(Long userId, Long id, MealSaveRequest request) {
        MealRecord record = getOwned(userId, id);
        LocalDate oldDate = record.getStatDate();

        LocalDateTime eatenAt = request.getEatenAt() == null ? record.getEatenAt() : request.getEatenAt();
        validateEatenAt(eatenAt);

        List<MealItem> items = buildItems(userId, request);
        if (items.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请至少提供一项食物，或填写自然语言描述");
        }

        mealItemMapper.delete(new LambdaQueryWrapper<MealItem>()
                .eq(MealItem::getMealRecordId, id));

        record.setMealType(request.getMealType());
        record.setEatenAt(eatenAt);
        record.setStatDate(eatenAt.toLocalDate());
        record.setRawInput(request.getRawInput());
        record.setNote(request.getNote());
        fillTotals(record, items);
        mealRecordMapper.updateById(record);

        saveItems(record.getId(), items);
        recomputeDailySummary(userId, oldDate);
        if (!oldDate.equals(record.getStatDate())) {
            recomputeDailySummary(userId, record.getStatDate());
        }
        return toVO(record, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        MealRecord record = getOwned(userId, id);
        mealRecordMapper.deleteById(id);
        recomputeDailySummary(userId, record.getStatDate());
    }

    @Override
    public MealVO get(Long userId, Long id) {
        MealRecord record = getOwned(userId, id);
        List<MealItem> items = listItems(List.of(id)).getOrDefault(id, List.of());
        return toVO(record, items);
    }

    @Override
    public List<MealVO> listByDate(Long userId, LocalDate date) {
        List<MealRecord> records = mealRecordMapper.selectList(new LambdaQueryWrapper<MealRecord>()
                .eq(MealRecord::getUserId, userId)
                .eq(MealRecord::getStatDate, date)
                .orderByAsc(MealRecord::getEatenAt));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, List<MealItem>> itemMap = listItems(records.stream().map(MealRecord::getId).toList());
        return records.stream()
                .map(r -> toVO(r, itemMap.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Override
    public PageResult<MealVO> history(Long userId, long page, long size) {
        Page<MealRecord> pageParam = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        Page<MealRecord> result = mealRecordMapper.selectPage(pageParam,
                new LambdaQueryWrapper<MealRecord>()
                        .eq(MealRecord::getUserId, userId)
                        .orderByDesc(MealRecord::getEatenAt));
        Map<Long, List<MealItem>> itemMap = result.getRecords().isEmpty()
                ? Map.of()
                : listItems(result.getRecords().stream().map(MealRecord::getId).toList());
        List<MealVO> list = result.getRecords().stream()
                .map(r -> toVO(r, itemMap.getOrDefault(r.getId(), List.of())))
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recomputeDailySummary(Long userId, LocalDate date) {
        List<MealRecord> records = mealRecordMapper.selectList(new LambdaQueryWrapper<MealRecord>()
                .eq(MealRecord::getUserId, userId)
                .eq(MealRecord::getStatDate, date));

        DailySummary summary = dailySummaryMapper.selectOne(new LambdaQueryWrapper<DailySummary>()
                .eq(DailySummary::getUserId, userId)
                .eq(DailySummary::getStatDate, date));

        if (records.isEmpty()) {
            if (summary != null) {
                dailySummaryMapper.deleteById(summary.getId());
            }
            return;
        }

        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        for (MealRecord record : records) {
            calories = calories.add(nvl(record.getTotalCalories()));
            protein = protein.add(nvl(record.getTotalProtein()));
            fat = fat.add(nvl(record.getTotalFat()));
            carbs = carbs.add(nvl(record.getTotalCarbs()));
        }

        Integer target = profileService.getDailyTarget(userId);
        boolean achieved = target != null && target > 0
                && Math.abs(calories.doubleValue() - target) <= target * 0.1;

        if (summary == null) {
            summary = new DailySummary();
            summary.setUserId(userId);
            summary.setStatDate(date);
            summary.setTotalCalories(calories);
            summary.setTotalProtein(protein);
            summary.setTotalFat(fat);
            summary.setTotalCarbs(carbs);
            summary.setMealCount(records.size());
            summary.setTargetCalories(target);
            summary.setIsAchieved(achieved ? 1 : 0);
            dailySummaryMapper.insert(summary);
        } else {
            summary.setTotalCalories(calories);
            summary.setTotalProtein(protein);
            summary.setTotalFat(fat);
            summary.setTotalCarbs(carbs);
            summary.setMealCount(records.size());
            summary.setTargetCalories(target);
            summary.setIsAchieved(achieved ? 1 : 0);
            dailySummaryMapper.updateById(summary);
        }
    }

    private List<MealItem> buildItems(Long userId, MealSaveRequest request) {
        List<MealItem> items = new ArrayList<>();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (MealItemRequest req : request.getItems()) {
                items.add(toEntity(req));
            }
            return items;
        }

        if (StringUtils.hasText(request.getRawInput())) {
            FoodParseRequest parseRequest = new FoodParseRequest();
            parseRequest.setText(request.getRawInput());
            parseRequest.setMealType(request.getMealType());
            FoodParseResultVO parsed = foodService.parse(userId, parseRequest);
            if (parsed.getItems() == null || parsed.getItems().isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "没有识别到食物，请换个说法或手动录入");
            }
            for (ParsedItemVO item : parsed.getItems()) {
                items.add(fromParsed(item));
            }
        }
        return items;
    }

    private MealItem toEntity(MealItemRequest req) {
        MealItem item = new MealItem();
        item.setFoodId(req.getFoodId());
        item.setFoodName(req.getFoodName());
        item.setQuantity(req.getQuantity());
        item.setUnit(req.getUnit());
        item.setGrams(req.getGrams());
        item.setCalories(nvl(req.getCalories()));
        item.setProtein(nvl(req.getProtein()));
        item.setFat(nvl(req.getFat()));
        item.setCarbs(nvl(req.getCarbs()));
        item.setConfidence(req.getConfidence());
        item.setSource(StringUtils.hasText(req.getSource()) ? req.getSource() : "MANUAL");
        return item;
    }

    private MealItem fromParsed(ParsedItemVO parsed) {
        MealItem item = new MealItem();
        item.setFoodId(parsed.getFoodId());
        item.setFoodName(parsed.getName());
        item.setQuantity(parsed.getQuantity());
        item.setUnit(parsed.getUnit());
        item.setGrams(parsed.getGrams());
        item.setCalories(nvl(parsed.getCalories()));
        item.setProtein(nvl(parsed.getProtein()));
        item.setFat(nvl(parsed.getFat()));
        item.setCarbs(nvl(parsed.getCarbs()));
        item.setConfidence(parsed.getConfidence());
        item.setSource("LLM");
        return item;
    }

    private void saveItems(Long mealRecordId, List<MealItem> items) {
        for (int i = 0; i < items.size(); i++) {
            MealItem item = items.get(i);
            item.setMealRecordId(mealRecordId);
            item.setSortOrder(i);
            mealItemMapper.insert(item);
        }
    }

    private void fillTotals(MealRecord record, List<MealItem> items) {
        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        for (MealItem item : items) {
            calories = calories.add(nvl(item.getCalories()));
            protein = protein.add(nvl(item.getProtein()));
            fat = fat.add(nvl(item.getFat()));
            carbs = carbs.add(nvl(item.getCarbs()));
        }
        record.setTotalCalories(calories);
        record.setTotalProtein(protein);
        record.setTotalFat(fat);
        record.setTotalCarbs(carbs);
    }

    private Map<Long, List<MealItem>> listItems(List<Long> mealRecordIds) {
        if (mealRecordIds.isEmpty()) {
            return Map.of();
        }
        List<MealItem> items = mealItemMapper.selectList(new LambdaQueryWrapper<MealItem>()
                .in(MealItem::getMealRecordId, mealRecordIds)
                .orderByAsc(MealItem::getSortOrder));
        return items.stream().collect(Collectors.groupingBy(MealItem::getMealRecordId));
    }

    private MealRecord getOwned(Long userId, Long id) {
        MealRecord record = mealRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "餐次记录不存在");
        }
        return record;
    }

    private void validateEatenAt(LocalDateTime eatenAt) {
        LocalDateTime now = LocalDateTime.now();
        if (eatenAt.isAfter(now.plusMinutes(10))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "进食时间不能晚于当前时间");
        }
        if (eatenAt.isBefore(now.minusDays(30))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能补录最近 30 天的记录");
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private MealVO toVO(MealRecord record, List<MealItem> items) {
        MealVO vo = new MealVO();
        vo.setId(record.getId());
        vo.setMealType(record.getMealType());
        vo.setMealTypeLabel(label(record.getMealType()));
        vo.setEatenAt(record.getEatenAt());
        vo.setStatDate(record.getStatDate());
        vo.setTotalCalories(record.getTotalCalories());
        vo.setTotalProtein(record.getTotalProtein());
        vo.setTotalFat(record.getTotalFat());
        vo.setTotalCarbs(record.getTotalCarbs());
        vo.setRawInput(record.getRawInput());
        vo.setNote(record.getNote());
        vo.setItems(items.stream().map(this::toItemVO).toList());
        return vo;
    }

    private MealItemVO toItemVO(MealItem item) {
        MealItemVO vo = new MealItemVO();
        vo.setId(item.getId());
        vo.setFoodId(item.getFoodId());
        vo.setFoodName(item.getFoodName());
        vo.setQuantity(item.getQuantity());
        vo.setUnit(item.getUnit());
        vo.setGrams(item.getGrams());
        vo.setCalories(item.getCalories());
        vo.setProtein(item.getProtein());
        vo.setFat(item.getFat());
        vo.setCarbs(item.getCarbs());
        vo.setConfidence(item.getConfidence());
        vo.setSource(item.getSource());
        return vo;
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
