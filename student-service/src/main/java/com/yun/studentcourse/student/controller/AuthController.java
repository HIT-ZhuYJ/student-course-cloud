package com.yun.studentcourse.student.controller;

import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.common.dto.LoginRequest;
import com.yun.studentcourse.common.dto.LoginResponse;
import com.yun.studentcourse.common.dto.RegisterRequest;
import com.yun.studentcourse.student.dto.StudentResponse;
import com.yun.studentcourse.student.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final StudentService studentService;

    public AuthController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/register")
    public Result<StudentResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(studentService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(studentService.login(request));
    }
}
