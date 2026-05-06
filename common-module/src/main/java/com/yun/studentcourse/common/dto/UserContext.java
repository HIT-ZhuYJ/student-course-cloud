package com.yun.studentcourse.common.dto;

import com.yun.studentcourse.common.RoleEnum;

public class UserContext {

    private Long userId;
    private String username;
    private RoleEnum role;
    private Long relatedId;

    public UserContext() {
    }

    public UserContext(Long userId, String username, RoleEnum role, Long relatedId) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.relatedId = relatedId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public RoleEnum getRole() {
        return role;
    }

    public void setRole(RoleEnum role) {
        this.role = role;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }
}
