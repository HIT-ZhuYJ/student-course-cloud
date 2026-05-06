package com.yun.studentcourse.common;

public class GlobalErrorResponse {

    private int code;
    private String message;
    private String path;
    private long timestamp;

    public GlobalErrorResponse() {
    }

    private GlobalErrorResponse(int code, String message, String path) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.timestamp = System.currentTimeMillis();
    }

    public static GlobalErrorResponse of(ErrorCode errorCode, String path) {
        return new GlobalErrorResponse(errorCode.getCode(), errorCode.getMessage(), path);
    }

    public static GlobalErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new GlobalErrorResponse(errorCode.getCode(), message, path);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
