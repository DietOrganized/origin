package com.example.calorie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.calorie.client.DeepSeekClient;
import com.example.calorie.client.LlmResult;
import com.example.calorie.common.BusinessException;
import com.example.calorie.common.PageResult;
import com.example.calorie.common.ResultCode;
import com.example.calorie.dto.RecipeRecommendRequest;
import com.example.calorie.entity.Recipe;
import com.example.calorie.entity.RecipeFavorite;
import com.example.calorie.mapper.RecipeFavoriteMapper;
import com.example.calorie.mapper.RecipeMapper;
import com.example.calorie.prompt.PromptLoader;
import com.example.calorie.service.ProfileService;
import com.example.calorie.service.RecipeService;
import com.example.calorie.service.StatsService;
import com.example.calorie.vo.RecipeVO;
import com.example.calorie.vo.TodayOverviewVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private static final String SYSTEM_PROMPT = "你是一名擅长中国家常菜的营养师，严格按照用户要求只输出 JSON，不要输出任何多余文字。";

    private final RecipeMapper recipeMapper;
    private final RecipeFavoriteMapper recipeFavoriteMapper;
    private final DeepSeekClient deepSeekClient;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;
    private final ProfileService profileService;
    private final StatsService statsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RecipeVO> recommend(Long userId, RecipeRecommendRequest request) {
        String mode = StringUtils.hasText(request.getMode()) ? request.getMode() : "INGREDIENT";
        boolean byIngredient = "INGREDIENT".equals(mode);
        if (byIngredient && (request.getIngredients() == null || request.getIngredients().isEmpty())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "按食材推荐时请至少输入一种食材");
        }

        int count = request.getCount() == null ? 2 : Math.min(Math.max(request.getCount(), 1), 3);
        Integer target = profileService.getDailyTarget(userId);
        BigDecimal remaining = BigDecimal.ZERO;
        if (target != null) {
            TodayOverviewVO today = statsService.today(userId);
            remaining = BigDecimal.valueOf(target).subtract(today.getTotalCalories());
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remaining = BigDecimal.ZERO;
            }
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("count", String.valueOf(count));
        variables.put("modeHint", byIngredient ? "按用户现有食材搭配菜谱" : "在热量上限内推荐菜谱");
        variables.put("ingredients", byIngredient
                ? String.join("、", request.getIngredients()) : "不限，自行搭配常见食材");
        variables.put("maxCalories", request.getMaxCalories() == null ? "0" : String.valueOf(request.getMaxCalories()));
        variables.put("mealType", label(request.getMealType()));
        variables.put("goal", goalLabel(userId));
        variables.put("remaining", remaining.setScale(0, RoundingMode.HALF_UP).toPlainString());

        String userPrompt = promptLoader.render("recipe_recommend", variables);
        LlmResult result = deepSeekClient.chat("RECIPE", userId, SYSTEM_PROMPT, userPrompt, 0.7, true);

        JsonNode root = readJson(result.getContent());
        JsonNode array = root.path("recipes");
        if (!array.isArray()) {
            throw new BusinessException(ResultCode.LLM_FORMAT_ERROR);
        }

        List<RecipeVO> list = new ArrayList<>();
        for (JsonNode node : array) {
            Recipe recipe = toEntity(node, userId, request, result.getModel());
            recipeMapper.insert(recipe);
            RecipeVO vo = toVO(recipe, false);
            list.add(vo);
        }
        if (list.isEmpty()) {
            throw new BusinessException(ResultCode.LLM_FORMAT_ERROR, "AI 没有生成有效菜谱，请重试");
        }
        return list;
    }

    @Override
    public PageResult<RecipeVO> list(Long userId, long page, long size, boolean favoriteOnly) {
        Page<Recipe> pageParam = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50));

        Set<Long> favoriteIds = favoriteIds(userId);
        if (favoriteOnly && favoriteIds.isEmpty()) {
            return PageResult.of(0, pageParam.getCurrent(), pageParam.getSize(), List.of());
        }

        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Recipe::getUserId, userId).or().eq(Recipe::getIsPublic, 1));
        if (favoriteOnly) {
            wrapper.in(Recipe::getId, favoriteIds);
        }
        wrapper.orderByDesc(Recipe::getId);

        Page<Recipe> result = recipeMapper.selectPage(pageParam, wrapper);
        List<RecipeVO> list = result.getRecords().stream()
                .map(r -> toVO(r, favoriteIds.contains(r.getId())))
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), list);
    }

    @Override
    public RecipeVO get(Long userId, Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null || !accessible(recipe, userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜谱不存在");
        }
        boolean favorite = recipeFavoriteMapper.selectCount(new LambdaQueryWrapper<RecipeFavorite>()
                .eq(RecipeFavorite::getUserId, userId)
                .eq(RecipeFavorite::getRecipeId, id)) > 0;
        return toVO(recipe, favorite);
    }

    @Override
    public void favorite(Long userId, Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null || !accessible(recipe, userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜谱不存在");
        }
        RecipeFavorite favorite = new RecipeFavorite();
        favorite.setUserId(userId);
        favorite.setRecipeId(id);
        try {
            recipeFavoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            log.debug("重复收藏，忽略: userId={}, recipeId={}", userId, id);
        }
    }

    @Override
    public void unfavorite(Long userId, Long id) {
        recipeFavoriteMapper.delete(new LambdaQueryWrapper<RecipeFavorite>()
                .eq(RecipeFavorite::getUserId, userId)
                .eq(RecipeFavorite::getRecipeId, id));
    }

    @Override
    public void delete(Long userId, Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜谱不存在");
        }
        if (recipe.getUserId() == null || !recipe.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己生成的菜谱");
        }
        recipeMapper.deleteById(id);
        recipeFavoriteMapper.delete(new LambdaQueryWrapper<RecipeFavorite>()
                .eq(RecipeFavorite::getRecipeId, id));
    }

    private Set<Long> favoriteIds(Long userId) {
        List<RecipeFavorite> favorites = recipeFavoriteMapper.selectList(
                new LambdaQueryWrapper<RecipeFavorite>().eq(RecipeFavorite::getUserId, userId));
        return favorites.stream().map(RecipeFavorite::getRecipeId).collect(Collectors.toCollection(HashSet::new));
    }

    private boolean accessible(Recipe recipe, Long userId) {
        return (recipe.getUserId() != null && recipe.getUserId().equals(userId))
                || (recipe.getIsPublic() != null && recipe.getIsPublic() == 1);
    }

    private Recipe toEntity(JsonNode node, Long userId, RecipeRecommendRequest request, String model) {
        Recipe recipe = new Recipe();
        recipe.setUserId(userId);
        recipe.setTitle(truncate(node.path("title").asText("未命名菜谱"), 100));
        recipe.setDescription(truncate(node.path("description").asText(""), 500));
        recipe.setServings(clampInt(node.path("servings").asInt(1), 1, 20));
        recipe.setCookingTimeMin(clampInt(node.path("cookingTimeMin").asInt(0), 0, 600));
        String difficulty = node.path("difficulty").asText("EASY");
        recipe.setDifficulty(List.of("EASY", "MEDIUM", "HARD").contains(difficulty) ? difficulty : "EASY");
        recipe.setIngredients(readIngredients(node.path("ingredients")));
        recipe.setSteps(readSteps(node.path("steps")));
        recipe.setTotalCalories(number(node, "totalCalories", 0, 5000));
        recipe.setTotalProtein(number(node, "protein", 0, 1000));
        recipe.setTotalFat(number(node, "fat", 0, 1000));
        recipe.setTotalCarbs(number(node, "carbs", 0, 1000));
        recipe.setTags(truncate(node.path("tags").asText(""), 255));
        recipe.setSourcePrompt(truncate(describe(request), 500));
        recipe.setModel(model);
        recipe.setIsPublic(0);
        return recipe;
    }

    private List<Map<String, Object>> readIngredients(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        try {
            return objectMapper.convertValue(node, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> readSteps(JsonNode node) {
        List<String> steps = new ArrayList<>();
        if (!node.isArray()) {
            return steps;
        }
        for (JsonNode item : node) {
            String text = item.asText("").trim();
            if (!text.isEmpty()) {
                steps.add(text);
            }
        }
        return steps;
    }

    private String describe(RecipeRecommendRequest request) {
        if (request.getIngredients() != null && !request.getIngredients().isEmpty()) {
            return "食材:" + String.join("、", request.getIngredients());
        }
        return "热量上限:" + (request.getMaxCalories() == null ? "不限" : request.getMaxCalories());
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
            log.error("AI 返回的菜谱不是合法 JSON: {}", content);
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
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String goalLabel(Long userId) {
        Integer target = profileService.getDailyTarget(userId);
        return target == null ? "未设置" : "每日目标 " + target + " kcal";
    }

    private String label(String mealType) {
        return switch (mealType == null ? "" : mealType) {
            case "BREAKFAST" -> "早餐";
            case "LUNCH" -> "午餐";
            case "DINNER" -> "晚餐";
            case "SNACK" -> "加餐";
            default -> "不限";
        };
    }

    private RecipeVO toVO(Recipe recipe, boolean favorite) {
        RecipeVO vo = new RecipeVO();
        vo.setId(recipe.getId());
        vo.setUserId(recipe.getUserId());
        vo.setTitle(recipe.getTitle());
        vo.setDescription(recipe.getDescription());
        vo.setServings(recipe.getServings());
        vo.setCookingTimeMin(recipe.getCookingTimeMin());
        vo.setDifficulty(recipe.getDifficulty());
        vo.setIngredients(recipe.getIngredients());
        vo.setSteps(recipe.getSteps());
        vo.setTotalCalories(recipe.getTotalCalories());
        vo.setTotalProtein(recipe.getTotalProtein());
        vo.setTotalFat(recipe.getTotalFat());
        vo.setTotalCarbs(recipe.getTotalCarbs());
        vo.setTags(recipe.getTags());
        vo.setIsPublic(recipe.getIsPublic());
        vo.setFavorite(favorite);
        vo.setCreatedAt(recipe.getCreatedAt());
        return vo;
    }
}
