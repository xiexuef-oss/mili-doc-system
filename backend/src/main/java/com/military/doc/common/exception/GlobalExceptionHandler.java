package com.military.doc.common.exception;

import com.military.doc.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("Business error: {} - {}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return Result.error("FORBIDDEN", "权限不足，无法访问该资源");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("JSON parse error: {}", e.getMessage());
        return Result.error("BAD_REQUEST", "请求体格式错误，请检查JSON格式");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getParameterName());
        return Result.error("BAD_REQUEST", "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<?> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage());
        return Result.error("CONFLICT", "数据冲突，该操作违反了数据完整性约束");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return Result.error("BAD_REQUEST", e.getMessage() != null ? e.getMessage() : "参数校验失败");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return Result.error("INTERNAL_ERROR", "服务器内部错误，请联系管理员");
    }
}
