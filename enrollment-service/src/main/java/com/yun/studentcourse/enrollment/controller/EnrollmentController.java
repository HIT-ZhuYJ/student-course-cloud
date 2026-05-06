package com.yun.studentcourse.enrollment.controller;

import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.enrollment.dto.EnrollmentCreateRequest;
import com.yun.studentcourse.enrollment.dto.EnrollmentResponse;
import com.yun.studentcourse.enrollment.dto.TimetableResponse;
import com.yun.studentcourse.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/enrollments")
    public Result<EnrollmentResponse> enroll(@Valid @RequestBody EnrollmentCreateRequest request) {
        return Result.success(enrollmentService.enroll(request));
    }

    @DeleteMapping("/enrollments/{enrollmentId}")
    public Result<EnrollmentResponse> drop(@PathVariable Long enrollmentId) {
        return Result.success(enrollmentService.drop(enrollmentId));
    }

    @GetMapping("/enrollments/students/{studentId}")
    public Result<List<EnrollmentResponse>> listStudentEnrollments(@PathVariable Long studentId) {
        return Result.success(enrollmentService.listStudentEnrollments(studentId));
    }

    @GetMapping("/enrollments/students/{studentId}/timetable")
    public Result<List<TimetableResponse>> getStudentTimetable(@PathVariable Long studentId) {
        return Result.success(enrollmentService.getStudentTimetable(studentId));
    }

    @GetMapping("/enrollments")
    public Result<PageResult<EnrollmentResponse>> listEnrollments(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String status
    ) {
        return Result.success(enrollmentService.listEnrollments(pageNo, pageSize, studentId, courseId, status));
    }
}
