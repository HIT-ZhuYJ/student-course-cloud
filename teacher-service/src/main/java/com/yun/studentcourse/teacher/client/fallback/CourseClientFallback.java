package com.yun.studentcourse.teacher.client.fallback;

import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.teacher.client.CourseClient;
import com.yun.studentcourse.teacher.client.dto.CourseScheduleResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CourseClientFallback implements CourseClient {

    @Override
    public Result<List<CourseScheduleResponse>> getSchedule(Long courseId) {
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, "course-service is unavailable, teacher schedule conflict was not checked");
    }
}
