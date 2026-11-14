package com.example.calorie.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdviceRequest {

    @Pattern(regexp = "WEEK|MONTH", message = "周期只能是 WEEK 或 MONTH")
    private String periodType = "WEEK";

    /** 可选：指定周期内任意一天，不传则取当前周/月 */
    private LocalDate periodStart;
}
