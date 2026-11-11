package com.example.calorie.common;

/**
 * 统一业务码。
 */
public enum ResultCode {

    SUCCESS(0, "success"),

    PARAM_ERROR(40001, "参数错误"),
    UNAUTHORIZED(40100, "未登录或登录已失效"),
    FORBIDDEN(40300, "无权访问"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源冲突"),
    TOO_MANY_REQUESTS(42900, "请求过于频繁，请稍后再试"),

    SERVER_ERROR(50000, "服务器开小差了，请稍后再试"),
    LLM_ERROR(50201, "AI 服务暂时不可用，请稍后重试或手动录入"),
    LLM_FORMAT_ERROR(50202, "AI 返回内容解析失败，请重试或手动录入");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
