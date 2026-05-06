package com.yun.studentcourse.teacher.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class TeacherCreateRequest {

    @NotBlank(message = "teacherNo is required")
    private String teacherNo;

    @NotBlank(message = "name is required")
    private String name;

    private String title;
    private String phone;

    @Email(message = "email format is invalid")
    private String email;

    private String status = "ACTIVE";

    public String getTeacherNo() {
        return teacherNo;
    }

    public void setTeacherNo(String teacherNo) {
        this.teacherNo = teacherNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
