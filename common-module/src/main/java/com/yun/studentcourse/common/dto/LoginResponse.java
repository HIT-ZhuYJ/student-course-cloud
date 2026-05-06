package com.yun.studentcourse.common.dto;

import com.yun.studentcourse.common.RoleEnum;

public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private RoleEnum role;
    private Long relatedId;
    private Long expiresAt;

    public LoginResponse() {
    }

    public LoginResponse(String token, Long userId, String username, RoleEnum role, Long relatedId, Long expiresAt) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.relatedId = relatedId;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
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

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
