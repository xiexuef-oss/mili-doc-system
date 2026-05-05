package com.military.doc.common.result;

import lombok.Data;

@Data
public class Result<T> {
    private String code;
    private String message;
    private T data;
    private String traceId;
    private String timestamp;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode("SUCCESS");
        result.setMessage("操作成功");
        result.setData(data);
        result.setTimestamp(java.time.Instant.now().toString());
        return result;
    }

    public static <T> Result<T> error(String code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(java.time.Instant.now().toString());
        return result;
    }
}