package com.example.calorie.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AdviceVO {

    private Long id;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String content;
    private Map<String, Object> metrics;
    private String model;
    private LocalDateTime createdAt;
}
