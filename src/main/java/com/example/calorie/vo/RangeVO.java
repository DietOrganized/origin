package com.example.calorie.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RangeVO {

    private LocalDate startDate;
    private LocalDate endDate;
}
