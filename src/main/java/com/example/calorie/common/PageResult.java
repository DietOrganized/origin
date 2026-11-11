package com.example.calorie.common;

import lombok.Data;

import java.util.List;

/**
 * 分页响应。
 */
@Data
public class PageResult<T> {

    private long total;
    private long page;
    private long size;
    private long pages;
    private List<T> list;

    public static <T> PageResult<T> of(long total, long page, long size, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.total = total;
        result.page = page;
        result.size = size;
        result.pages = size <= 0 ? 0 : (total + size - 1) / size;
        result.list = list;
        return result;
    }
}
