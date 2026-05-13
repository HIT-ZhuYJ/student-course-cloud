package com.yun.studentcourse.student.controller;

import com.yun.studentcourse.common.BusinessException;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.student.dto.AccountStatusUpdateRequest;
import com.yun.studentcourse.student.dto.StudentResponse;
import com.yun.studentcourse.student.dto.StudentStatusResponse;
import com.yun.studentcourse.student.dto.StudentUpdateRequest;
import com.yun.studentcourse.student.dto.TeacherAccountCreateRequest;
import com.yun.studentcourse.student.dto.TeacherAccountResponse;
import com.yun.studentcourse.student.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/students/{studentId}")
    public Result<StudentResponse> getStudent(@PathVariable Long studentId) {
        return Result.success(studentService.getStudent(studentId));
    }

    @PutMapping("/students/{studentId}")
    public Result<StudentResponse> updateStudent(
            @PathVariable Long studentId,
            @Valid @RequestBody StudentUpdateRequest request
    ) {
        return Result.success(studentService.updateStudent(studentId, request));
    }

    @GetMapping("/students")
    public Result<PageResult<StudentResponse>> listStudents(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return Result.success(studentService.listStudents(pageNo, pageSize, keyword));
    }

    @GetMapping("/internal/students/{studentId}/status")
    public Result<StudentStatusResponse> getStudentStatus(@PathVariable Long studentId) {
        return Result.success(studentService.getStudentStatus(studentId));
    }

    @PostMapping("/internal/accounts/teachers")
    public Result<TeacherAccountResponse> createTeacherAccount(@Valid @RequestBody TeacherAccountCreateRequest request) {
        try {
            return Result.success(studentService.createTeacherAccount(request));
        } catch (BusinessException ex) {
            return Result.fail(ex.getErrorCode(), ex.getMessage());
        }
    }

    @PutMapping("/internal/accounts/teachers/{teacherId}/status")
    public Result<Void> updateTeacherAccountStatus(
            @PathVariable Long teacherId,
            @Valid @RequestBody AccountStatusUpdateRequest request
    ) {
        try {
            studentService.updateTeacherAccountStatus(teacherId, request);
            return Result.success();
        } catch (BusinessException ex) {
            return Result.fail(ex.getErrorCode(), ex.getMessage());
        }
    }
}
