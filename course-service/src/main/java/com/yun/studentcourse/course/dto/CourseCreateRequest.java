package com.yun.studentcourse.course.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CourseCreateRequest {

    @NotBlank(message = "courseCode is required")
    private String courseCode;

    @NotBlank(message = "courseName is required")
    private String courseName;

    @NotNull(message = "credit is required")
    @DecimalMin(value = "0.5", message = "credit must be positive")
    private BigDecimal credit;

    @Min(value = 1, message = "capacity must be at least 1")
    private int capacity;

    private String status = "OPEN";
    private String description;

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
