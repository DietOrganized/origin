package com.example.calorie.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.calorie.common.BusinessException;
import com.example.calorie.common.ResultCode;
import com.example.calorie.config.DeepSeekProperties;
import com.example.calorie.entity.LlmCallLog;
import com.example.calorie.mapper.LlmCallLogMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 调用客户端：超时控制、失败重试 1 次、每日限额、调用日志。
 * 接口与 OpenAI Chat Completions 兼容。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekClient {

    private final DeepSeekProperties properties;
    private final LlmCallLogMapper llmCallLogMapper;
    private final ObjectMapper objectMapper;

    private RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public LlmResult chat(String scene, Long userId, String systemPrompt, String userPrompt,
                          double temperature, boolean jsonMode) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(ResultCode.LLM_ERROR, "未配置 DeepSeek API Key");
        }
        checkQuota(userId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
        body.put("temperature", temperature);
        body.put("stream", false);
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        long start = System.currentTimeMillis();
        String errorMessage = null;

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String response = restClient.post()
                        .uri("/chat/completions")
                        .body(body)
                        .retrieve()
                        .body(String.class);
                JsonNode root = objectMapper.readTree(response);
                LlmResult result = new LlmResult();
                result.setContent(root.path("choices").path(0).path("message").path("content").asText(""));
                result.setModel(root.path("model").asText(properties.getModel()));
                JsonNode usage = root.path("usage");
                result.setPromptTokens(usage.path("prompt_tokens").asInt(0));
                result.setCompletionTokens(usage.path("completion_tokens").asInt(0));
                result.setTotalTokens(usage.path("total_tokens").asInt(0));
                result.setLatencyMs(System.currentTimeMillis() - start);

                if (!StringUtils.hasText(result.getContent())) {
                    throw new IllegalStateException("AI 返回内容为空");
                }
                saveLog(userId, scene, result, true, null);
                return result;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                log.warn("DeepSeek 调用失败（第 {} 次）: {}", attempt, errorMessage);
                if (attempt == 1) {
                    sleep(1000);
                }
            }
        }

        saveLog(userId, scene, null, false, errorMessage);
        throw new BusinessException(ResultCode.LLM_ERROR);
    }

    private void checkQuota(Long userId) {
        if (userId == null) {
            return;
        }
        Long count = llmCallLogMapper.selectCount(new LambdaQueryWrapper<LlmCallLog>()
                .eq(LlmCallLog::getUserId, userId)
                .ge(LlmCallLog::getCreatedAt, LocalDate.now().atStartOfDay()));
        if (count != null && count >= properties.getDailyLimit()) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS,
                    "今日 AI 调用次数已达上限（" + properties.getDailyLimit() + " 次），请明天再试");
        }
    }

    private void saveLog(Long userId, String scene, LlmResult result, boolean success, String error) {
        try {
            LlmCallLog entity = new LlmCallLog();
            entity.setUserId(userId);
            entity.setScene(scene);
            entity.setModel(result == null ? properties.getModel() : result.getModel());
            entity.setPromptTokens(result == null ? 0 : result.getPromptTokens());
            entity.setCompletionTokens(result == null ? 0 : result.getCompletionTokens());
            entity.setTotalTokens(result == null ? 0 : result.getTotalTokens());
            entity.setLatencyMs(result == null ? 0 : (int) result.getLatencyMs());
            entity.setSuccess(success ? 1 : 0);
            entity.setErrorMsg(error == null ? null : truncate(error, 480));
            entity.setCreatedAt(LocalDateTime.now());
            llmCallLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("写入 LLM 调用日志失败", e);
        }
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
