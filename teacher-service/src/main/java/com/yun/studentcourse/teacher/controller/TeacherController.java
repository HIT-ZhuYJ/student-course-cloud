package com.yun.studentcourse.teacher.controller;

import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.teacher.dto.CourseTeacherAssignedResponse;
import com.yun.studentcourse.teacher.dto.TeacherCourseAssignmentResponse;
import com.yun.studentcourse.teacher.dto.TeacherCreateRequest;
import com.yun.studentcourse.teacher.dto.TeacherResponse;
import com.yun.studentcourse.teacher.dto.TeacherUpdateRequest;
import com.yun.studentcourse.teacher.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping("/teachers")
    public Result<TeacherResponse> createTeacher(@Valid @RequestBody TeacherCreateRequest request) {
        return Result.success(teacherService.createTeacher(request));
    }

    @PutMapping("/teachers/{teacherId}")
    public Result<TeacherResponse> updateTeacher(
            @PathVariable Long teacherId,
            @Valid @RequestBody TeacherUpdateRequest request
    ) {
        return Result.success(teacherService.updateTeacher(teacherId, request));
    }

    @DeleteMapping("/teachers/{teacherId}")
    public Result<Void> disableTeacher(@PathVariable Long teacherId) {
        teacherService.disableTeacher(teacherId);
        return Result.success();
    }

    @GetMapping("/teachers/{teacherId}")
    public Result<TeacherResponse> getTeacher(@PathVariable Long teacherId) {
        return Result.success(teacherService.getTeacher(teacherId));
    }

    @GetMapping("/teachers")
    public Result<PageResult<TeacherResponse>> listTeachers(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return Result.success(teacherService.listTeachers(pageNo, pageSize, keyword, status));
    }

    @PostMapping("/teachers/{teacherId}/courses/{courseId}")
    public Result<TeacherCourseAssignmentResponse> assignCourse(
            @PathVariable Long teacherId,
            @PathVariable Long courseId
    ) {
        return Result.success(teacherService.assignCourse(teacherId, courseId));
    }

    @DeleteMapping("/teachers/{teacherId}/courses/{courseId}")
    public Result<Void> cancelCourseAssignment(
            @PathVariable Long teacherId,
            @PathVariable Long courseId
    ) {
        teacherService.cancelCourseAssignment(teacherId, courseId);
        return Result.success();
    }

    @GetMapping("/teachers/{teacherId}/courses")
    public Result<List<TeacherCourseAssignmentResponse>> listTeacherCourses(@PathVariable Long teacherId) {
        return Result.success(teacherService.listTeacherCourses(teacherId));
    }

    @GetMapping("/internal/courses/{courseId}/teacher-assigned")
    public Result<CourseTeacherAssignedResponse> isCourseTeacherAssigned(@PathVariable Long courseId) {
        return Result.success(teacherService.isCourseTeacherAssigned(courseId));
    }
}
