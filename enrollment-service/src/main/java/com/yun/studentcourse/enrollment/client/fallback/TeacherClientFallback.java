package com.yun.studentcourse.enrollment.client.fallback;

import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.enrollment.client.TeacherClient;
import com.yun.studentcourse.enrollment.client.dto.CourseTeacherAssignedResponse;
import com.yun.studentcourse.enrollment.client.dto.TeacherCourseAssignmentResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeacherClientFallback implements TeacherClient {

    @Override
    public Result<CourseTeacherAssignedResponse> isCourseTeacherAssigned(Long courseId) {
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, "teacher-service is unavailable, please try again later");
    }

    @Override
    public Result<List<TeacherCourseAssignmentResponse>> listTeacherCourses(Long teacherId) {
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, "teacher-service is unavailable, please try again later");
    }
}
