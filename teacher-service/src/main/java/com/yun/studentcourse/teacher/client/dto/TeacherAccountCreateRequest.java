package com.yun.studentcourse.teacher.client.dto;

public class TeacherAccountCreateRequest {

    private String username;
    private String password;
    private Long teacherId;
    private String status;

    public TeacherAccountCreateRequest() {
    }

    public TeacherAccountCreateRequest(String username, String password, Long teacherId, String status) {
        this.username = username;
        this.password = password;
        this.teacherId = teacherId;
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
