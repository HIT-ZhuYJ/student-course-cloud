package com.yun.studentcourse.common;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = errorCode.getMessage();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.detail = message;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getCode() {
        return errorCode.getCode();
    }

    public String getDetail() {
        return detail;
    }
}
