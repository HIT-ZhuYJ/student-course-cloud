package com.yun.studentcourse.enrollment.client.fallback;

import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.enrollment.client.StudentClient;
import com.yun.studentcourse.enrollment.client.dto.StudentStatusResponse;
import org.springframework.stereotype.Component;

@Component
public class StudentClientFallback implements StudentClient {

    @Override
    public Result<StudentStatusResponse> getStudentStatus(Long studentId) {
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, "student-service is unavailable, please try again later");
    }
}
