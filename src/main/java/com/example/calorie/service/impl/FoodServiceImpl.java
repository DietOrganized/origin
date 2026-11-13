package com.example.calorie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.calorie.client.DeepSeekClient;
import com.example.calorie.client.LlmResult;
import com.example.calorie.common.BusinessException;
import com.example.calorie.common.PageResult;
import com.example.calorie.common.ResultCode;
import com.example.calorie.dto.FoodParseRequest;
import com.example.calorie.entity.Food;
import com.example.calorie.mapper.FoodMapper;
import com.example.calorie.prompt.PromptLoader;
import com.example.calorie.service.FoodService;
import com.example.calorie.vo.FoodParseResultVO;
import com.example.calorie.vo.FoodVO;
import com.example.calorie.vo.ParsedItemVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {

    private static final String SYSTEM_PROMPT = "你是一名专业的中国营养师，严格按照用户要求只输出 JSON，不要输出任何多余文字。";

    private final FoodMapper foodMapper;
    private final DeepSeekClient deepSeekClient;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;

    @Override
    public FoodParseResultVO parse(Long userId, FoodParseRequest request) {
        String text = request.getText().trim();

        Food cached = findExact(text);
        if (cached != null) {
            return fromCache(cached);
        }

        String userPrompt = promptLoader.render("food_parse", Map.of("input", text));
        LlmResult result = deepSeekClient.chat("FOOD_PARSE", userId, SYSTEM_PROMPT, userPrompt, 0.2, true);

        JsonNode root = readJson(result.getContent());
        JsonNode itemsNode = root.path("items");
        if (!itemsNode.isArray()) {
            throw new BusinessException(ResultCode.LLM_FORMAT_ERROR);
        }

        List<ParsedItemVO> items = new ArrayList<>();
        for (JsonNode node : itemsNode) {
            ParsedItemVO item = toItem(node);
            if (item != null) {
                items.add(item);
                upsertFood(item, userId);
            }
        }

        FoodParseResultVO vo = new FoodParseResultVO();
        vo.setItems(items);
        vo.setTotalCalories(sum(items, "calories"));
        vo.setTotalProtein(sum(items, "protein"));
        vo.setTotalFat(sum(items, "fat"));
        vo.setTotalCarbs(sum(items, "carbs"));
        vo.setNote(root.path("note").asText(""));
        vo.setFromCache(false);
        return vo;
    }

    @Override
    public PageResult<FoodVO> search(String keyword, long page, long size) {
        Page<Food> pageParam = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Food::getName, kw)
                    .or().like(Food::getAlias, kw)
                    .or().like(Food::getCategory, kw));
        }
        wrapper.orderByDesc(Food::getVerified).orderByAsc(Food::getId);
        Page<Food> result = foodMapper.selectPage(pageParam, wrapper);
        List<FoodVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), list);
    }

    @Override
    public FoodVO getById(Long id) {
        Food food = foodMapper.selectById(id);
        if (food == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "食物不存在");
        }
        return toVO(food);
    }

    private Food findExact(String text) {
        Food byName = foodMapper.selectOne(new LambdaQueryWrapper<Food>()
                .eq(Food::getName, text).last("LIMIT 1"));
        if (byName != null) {
            return byName;
        }
        // 别名按逗号精确匹配，避免「蛋」误命中「鸡蛋」
        return foodMapper.selectOne(new LambdaQueryWrapper<Food>()
                .apply("FIND_IN_SET({0}, `alias`) > 0", text).last("LIMIT 1"));
    }

    private FoodParseResultVO fromCache(Food food) {
        BigDecimal grams = food.getDefaultUnitGrams() != null
                ? food.getDefaultUnitGrams() : BigDecimal.valueOf(100);
        ParsedItemVO item = new ParsedItemVO();
        item.setFoodId(food.getId());
        item.setName(food.getName());
        item.setQuantity(BigDecimal.ONE);
        item.setUnit(StringUtils.hasText(food.getDefaultUnit()) ? food.getDefaultUnit() : "份");
        item.setGrams(scale(grams, 1));
        item.setCalories(portion(food.getCaloriesPer100g(), grams));
        item.setProtein(portion(food.getProteinPer100g(), grams));
        item.setFat(portion(food.getFatPer100g(), grams));
        item.setCarbs(portion(food.getCarbsPer100g(), grams));
        item.setConfidence(BigDecimal.valueOf(0.95));
        item.setSource("CACHE");

        FoodParseResultVO vo = new FoodParseResultVO();
        vo.setItems(List.of(item));
        vo.setTotalCalories(item.getCalories());
        vo.setTotalProtein(item.getProtein());
        vo.setTotalFat(item.getFat());
        vo.setTotalCarbs(item.getCarbs());
        vo.setNote("命中本地食物库，未调用 AI");
        vo.setFromCache(true);
        return vo;
    }

    private ParsedItemVO toItem(JsonNode node) {
        String name = node.path("name").asText("").trim();
        if (!StringUtils.hasText(name)) {
            return null;
        }
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }

        ParsedItemVO item = new ParsedItemVO();
        item.setName(name);
        item.setQuantity(number(node, "quantity", 0.01, 99));
        String unit = node.path("unit").asText("份");
        item.setUnit(StringUtils.hasText(unit) ? unit : "份");
        item.setGrams(number(node, "grams", 0.1, 5000));
        item.setCalories(number(node, "calories", 0, 5000));
        item.setProtein(number(node, "protein", 0, 1000));
        item.setFat(number(node, "fat", 0, 1000));
        item.setCarbs(number(node, "carbs", 0, 1000));
        item.setConfidence(number(node, "confidence", 0, 1));
        item.setSource("LLM");
        return item;
    }

    /**
     * 把 LLM 解析结果沉淀到食物库，下次同名可直接命中，省 token 也更稳定。
     */
    private void upsertFood(ParsedItemVO item, Long userId) {
        if (item.getGrams() == null || item.getGrams().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Long exists = foodMapper.selectCount(new LambdaQueryWrapper<Food>()
                .eq(Food::getName, item.getName()));
        if (exists != null && exists > 0) {
            return;
        }
        Food food = new Food();
        food.setName(item.getName());
        food.setCategory("AI识别");
        food.setCaloriesPer100g(per100(item.getCalories(), item.getGrams()));
        food.setProteinPer100g(per100(item.getProtein(), item.getGrams()));
        food.setFatPer100g(per100(item.getFat(), item.getGrams()));
        food.setCarbsPer100g(per100(item.getCarbs(), item.getGrams()));
        food.setDefaultUnit(item.getUnit());
        food.setDefaultUnitGrams(scale(item.getGrams(), 1));
        food.setSource("LLM");
        food.setVerified(0);
        food.setCreatedBy(userId);
        try {
            foodMapper.insert(food);
        } catch (Exception e) {
            log.warn("食物入库失败（可能并发重复）: {}", item.getName());
        }
    }

    private JsonNode readJson(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            int idx = text.indexOf('\n');
            if (idx > 0) {
                text = text.substring(idx + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        try {
            return objectMapper.readTree(text.trim());
        } catch (Exception e) {
            log.error("AI 返回内容不是合法 JSON: {}", content);
            throw new BusinessException(ResultCode.LLM_FORMAT_ERROR);
        }
    }

    private BigDecimal number(JsonNode node, String field, double min, double max) {
        double value = node.path(field).asDouble(0);
        if (Double.isNaN(value) || value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 由「每 100g 含量」换算成实际份量。
     */
    private BigDecimal portion(BigDecimal per100g, BigDecimal grams) {
        if (per100g == null || grams == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return per100g.multiply(grams)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * 由「实际份量」反推每 100g 含量，用于把 AI 结果沉淀进食物库。
     */
    private BigDecimal per100(BigDecimal value, BigDecimal grams) {
        if (value == null || grams == null || grams.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(grams, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? BigDecimal.ZERO : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(List<ParsedItemVO> items, String field) {
        BigDecimal total = BigDecimal.ZERO;
        for (ParsedItemVO item : items) {
            BigDecimal value = switch (field) {
                case "protein" -> item.getProtein();
                case "fat" -> item.getFat();
                case "carbs" -> item.getCarbs();
                default -> item.getCalories();
            };
            total = total.add(value == null ? BigDecimal.ZERO : value);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private FoodVO toVO(Food food) {
        FoodVO vo = new FoodVO();
        vo.setId(food.getId());
        vo.setName(food.getName());
        vo.setAlias(food.getAlias());
        vo.setCategory(food.getCategory());
        vo.setCaloriesPer100g(food.getCaloriesPer100g());
        vo.setProteinPer100g(food.getProteinPer100g());
        vo.setFatPer100g(food.getFatPer100g());
        vo.setCarbsPer100g(food.getCarbsPer100g());
        vo.setDefaultUnit(food.getDefaultUnit());
        vo.setDefaultUnitGrams(food.getDefaultUnitGrams());
        vo.setSource(food.getSource());
        vo.setVerified(food.getVerified());
        return vo;
    }
}
