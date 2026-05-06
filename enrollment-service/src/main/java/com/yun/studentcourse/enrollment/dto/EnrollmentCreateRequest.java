package com.yun.studentcourse.enrollment.dto;

import jakarta.validation.constraints.NotNull;

public class EnrollmentCreateRequest {

    @NotNull(message = "studentId is required")
    private Long studentId;

    @NotNull(message = "courseId is required")
    private Long courseId;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
}
