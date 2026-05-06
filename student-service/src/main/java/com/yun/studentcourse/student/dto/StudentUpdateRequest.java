package com.yun.studentcourse.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class StudentUpdateRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "major is required")
    private String major;

    @NotBlank(message = "grade is required")
    private String grade;

    private String phone;

    @Email(message = "email format is invalid")
    private String email;

    private String status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
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
