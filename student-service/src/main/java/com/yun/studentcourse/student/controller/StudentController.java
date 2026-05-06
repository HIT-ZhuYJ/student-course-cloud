package com.yun.studentcourse.student.controller;

import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.student.dto.StudentResponse;
import com.yun.studentcourse.student.dto.StudentStatusResponse;
import com.yun.studentcourse.student.dto.StudentUpdateRequest;
import com.yun.studentcourse.student.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
