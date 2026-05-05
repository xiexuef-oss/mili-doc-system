package com.military.doc.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException("RESOURCE_NOT_FOUND", message);
    }

    public static BusinessException validation(String message) {
        return new BusinessException("VALIDATION_ERROR", message);
    }

    public static BusinessException versionConflict() {
        return new BusinessException("VERSION_CONFLICT", "版本已变更，请刷新后重试");
    }

    public static BusinessException documentLocked() {
        return new BusinessException("DOCUMENT_LOCKED", "文档已被锁定");
    }
}