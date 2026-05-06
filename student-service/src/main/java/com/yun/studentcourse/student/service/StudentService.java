package com.yun.studentcourse.student.service;

import com.yun.studentcourse.common.dto.LoginRequest;
import com.yun.studentcourse.common.dto.LoginResponse;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.common.dto.RegisterRequest;
import com.yun.studentcourse.student.dto.StudentResponse;
import com.yun.studentcourse.student.dto.StudentStatusResponse;
import com.yun.studentcourse.student.dto.StudentUpdateRequest;

public interface StudentService {

    StudentResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    StudentResponse getStudent(Long studentId);

    StudentResponse updateStudent(Long studentId, StudentUpdateRequest request);

    PageResult<StudentResponse> listStudents(int pageNo, int pageSize, String keyword);

    StudentStatusResponse getStudentStatus(Long studentId);
}
