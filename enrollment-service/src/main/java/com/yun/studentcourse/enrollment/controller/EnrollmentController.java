package com.yun.studentcourse.enrollment.controller;

import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.common.BusinessException;
import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.enrollment.dto.EnrollmentCreateRequest;
import com.yun.studentcourse.enrollment.dto.EnrollmentResponse;
import com.yun.studentcourse.enrollment.dto.TeacherCourseStudentResponse;
import com.yun.studentcourse.enrollment.dto.TimetableResponse;
import com.yun.studentcourse.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EnrollmentController {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_TEACHER = "TEACHER";

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/enrollments")
    public Result<EnrollmentResponse> enroll(
            @RequestHeader("X-Role") String role,
            @RequestHeader(value = "X-Related-Id", required = false) Long relatedId,
            @Valid @RequestBody EnrollmentCreateRequest request
    ) {
        requireStudentOwnerOrAdmin(role, relatedId, request.getStudentId());
        return Result.success(enrollmentService.enroll(request));
    }

    @DeleteMapping("/enrollments/{enrollmentId}")
    public Result<EnrollmentResponse> drop(
            @RequestHeader("X-Role") String role,
            @RequestHeader(value = "X-Related-Id", required = false) Long relatedId,
            @PathVariable Long enrollmentId
    ) {
        return Result.success(enrollmentService.drop(enrollmentId, requesterStudentId(role, relatedId), isAdmin(role)));
    }

    @GetMapping("/enrollments/students/{studentId}")
    public Result<List<EnrollmentResponse>> listStudentEnrollments(
            @RequestHeader("X-Role") String role,
            @RequestHeader(value = "X-Related-Id", required = false) Long relatedId,
            @PathVariable Long studentId
    ) {
        requireStudentOwnerOrAdmin(role, relatedId, studentId);
        return Result.success(enrollmentService.listStudentEnrollments(studentId));
    }

    @GetMapping("/enrollments/students/{studentId}/timetable")
    public Result<List<TimetableResponse>> getStudentTimetable(
            @RequestHeader("X-Role") String role,
            @RequestHeader(value = "X-Related-Id", required = false) Long relatedId,
            @PathVariable Long studentId
    ) {
        requireStudentOwnerOrAdmin(role, relatedId, studentId);
        return Result.success(enrollmentService.getStudentTimetable(studentId));
    }

    @GetMapping("/enrollments/teachers/{teacherId}/courses/{courseId}/students")
    public Result<List<TeacherCourseStudentResponse>> listTeacherCourseStudents(
            @RequestHeader("X-Role") String role,
            @RequestHeader(value = "X-Related-Id", required = false) Long relatedId,
            @PathVariable Long teacherId,
            @PathVariable Long courseId
    ) {
        requireTeacherOwnerOrAdmin(role, relatedId, teacherId);
        return Result.success(enrollmentService.listTeacherCourseStudents(teacherId, courseId));
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

    private void requireStudentOwnerOrAdmin(String role, Long relatedId, Long targetStudentId) {
        if (isAdmin(role)) {
            return;
        }
        if (ROLE_STUDENT.equals(role) && relatedId != null && relatedId.equals(targetStudentId)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "students can only operate their own enrollments");
    }

    private void requireTeacherOwnerOrAdmin(String role, Long relatedId, Long targetTeacherId) {
        if (isAdmin(role)) {
            return;
        }
        if (ROLE_TEACHER.equals(role) && relatedId != null && relatedId.equals(targetTeacherId)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "teachers can only view their own course students");
    }

    private Long requesterStudentId(String role, Long relatedId) {
        return ROLE_STUDENT.equals(role) ? relatedId : null;
    }

    private boolean isAdmin(String role) {
        return ROLE_ADMIN.equals(role);
    }
}
