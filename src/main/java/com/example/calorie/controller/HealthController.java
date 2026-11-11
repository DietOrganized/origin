package com.example.calorie.controller;

import com.example.calorie.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Tag(name = "00. 健康检查")
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "服务与数据库连通性检查")
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", "calorie-app");
        try {
            String dbTime = jdbcTemplate.queryForObject("SELECT DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')",
                    String.class);
            data.put("status", "UP");
            data.put("dbTime", dbTime);
        } catch (Exception e) {
            log.error("数据库连通性检查失败", e);
            data.put("status", "DB_DOWN");
            data.put("dbError", e.getMessage());
        }
        return Result.ok(data);
    }
}
