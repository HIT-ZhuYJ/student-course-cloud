package com.yun.studentcourse.common;

public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    CONFLICT(409, "conflict"),
    VALIDATION_ERROR(422, "validation error"),
    BUSINESS_ERROR(4000, "business error"),
    REMOTE_SERVICE_ERROR(5001, "remote service error"),
    SERVICE_UNAVAILABLE(5003, "service unavailable"),
    SYSTEM_ERROR(5000, "system error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
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
