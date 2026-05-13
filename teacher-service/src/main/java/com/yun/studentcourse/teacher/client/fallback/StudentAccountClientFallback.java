package com.yun.studentcourse.teacher.client.fallback;

import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.teacher.client.StudentAccountClient;
import com.yun.studentcourse.teacher.client.dto.AccountStatusUpdateRequest;
import com.yun.studentcourse.teacher.client.dto.TeacherAccountCreateRequest;
import com.yun.studentcourse.teacher.client.dto.TeacherAccountResponse;
import org.springframework.stereotype.Component;

@Component
public class StudentAccountClientFallback implements StudentAccountClient {

    @Override
    public Result<TeacherAccountResponse> createTeacherAccount(TeacherAccountCreateRequest request) {
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, "student-service is unavailable, teacher account was not created");
    }

    @Override
    public Result<Void> updateTeacherAccountStatus(Long teacherId, AccountStatusUpdateRequest request) {
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, "student-service is unavailable, teacher account status was not updated");
    }
}
