package com.yun.studentcourse.teacher.client.dto;

public class AccountStatusUpdateRequest {

    private String status;

    public AccountStatusUpdateRequest() {
    }

    public AccountStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
