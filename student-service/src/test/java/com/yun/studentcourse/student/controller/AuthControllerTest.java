package com.yun.studentcourse.student.controller;

import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.common.RoleEnum;
import com.yun.studentcourse.common.dto.LoginRequest;
import com.yun.studentcourse.common.dto.LoginResponse;
import com.yun.studentcourse.common.dto.RegisterRequest;
import com.yun.studentcourse.student.dto.StudentResponse;
import com.yun.studentcourse.student.service.StudentService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final StudentService studentService = mock(StudentService.class);
    private final AuthController controller = new AuthController(studentService);

    @Test
    void loginDelegatesToStudentServiceAndReturnsToken() {
        LoginRequest request = new LoginRequest("alice", "secret123");
        LoginResponse response = new LoginResponse("token", 1L, "alice", RoleEnum.STUDENT, 1001L, 123L);
        when(studentService.login(request)).thenReturn(response);

        Result<LoginResponse> result = controller.login(request);

        assertThat(result.getCode()).isEqualTo(ErrorCode.SUCCESS.getCode());
        assertThat(result.getData().getToken()).isEqualTo("token");
        verify(studentService).login(request);
    }

    @Test
    void registerDelegatesToStudentService() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        StudentResponse student = new StudentResponse();
        student.setStudentId(1001L);
        when(studentService.register(request)).thenReturn(student);

        Result<StudentResponse> result = controller.register(request);

        assertThat(result.getData().getStudentId()).isEqualTo(1001L);
        verify(studentService).register(request);
    }
}
