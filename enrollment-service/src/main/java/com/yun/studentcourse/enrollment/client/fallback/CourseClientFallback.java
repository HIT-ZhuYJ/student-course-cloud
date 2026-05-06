package com.yun.studentcourse.enrollment.client.fallback;

import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.enrollment.client.CourseClient;
import com.yun.studentcourse.enrollment.client.dto.CourseCapacityResponse;
import com.yun.studentcourse.enrollment.client.dto.CourseScheduleResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CourseClientFallback implements CourseClient {

    private static final String MESSAGE = "course-service is unavailable, please try again later";

    @Override
    public Result<CourseCapacityResponse> checkCapacity(Long courseId) {
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, MESSAGE);
    }

    @Override
    public Result<List<CourseScheduleResponse>> getSchedule(Long courseId) {
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, MESSAGE);
    }

    @Override
    public Result<CourseCapacityResponse> increaseSelectedCount(Long courseId) {
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, MESSAGE);
    }

    @Override
    public Result<CourseCapacityResponse> decreaseSelectedCount(Long courseId) {
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, MESSAGE);
    }
}
