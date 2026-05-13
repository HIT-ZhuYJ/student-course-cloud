package com.yun.studentcourse.teacher.client;

import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.teacher.client.dto.AccountStatusUpdateRequest;
import com.yun.studentcourse.teacher.client.dto.TeacherAccountCreateRequest;
import com.yun.studentcourse.teacher.client.dto.TeacherAccountResponse;
import com.yun.studentcourse.teacher.client.fallback.StudentAccountClientFallback;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "student-service",
        contextId = "StudentAccountClient",
        fallback = StudentAccountClientFallback.class
)
public interface StudentAccountClient {

    @PostMapping("/internal/accounts/teachers")
    Result<TeacherAccountResponse> createTeacherAccount(@Valid @RequestBody TeacherAccountCreateRequest request);

    @PutMapping("/internal/accounts/teachers/{teacherId}/status")
    Result<Void> updateTeacherAccountStatus(
            @PathVariable("teacherId") Long teacherId,
            @Valid @RequestBody AccountStatusUpdateRequest request
    );
}
