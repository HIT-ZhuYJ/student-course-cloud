package com.yun.studentcourse.enrollment.client;

import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.enrollment.client.dto.StudentResponse;
import com.yun.studentcourse.enrollment.client.dto.StudentStatusResponse;
import com.yun.studentcourse.enrollment.client.fallback.StudentClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "student-service", contextId = "StudentClient", fallback = StudentClientFallback.class)
public interface StudentClient {

    @GetMapping("/internal/students/{studentId}/status")
    Result<StudentStatusResponse> getStudentStatus(@PathVariable("studentId") Long studentId);

    @GetMapping("/students/{studentId}")
    Result<StudentResponse> getStudent(@PathVariable("studentId") Long studentId);
}
