package com.yun.studentcourse.enrollment.client;

import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.enrollment.client.dto.CourseTeacherAssignedResponse;
import com.yun.studentcourse.enrollment.client.fallback.TeacherClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "teacher-service", contextId = "TeacherClient", fallback = TeacherClientFallback.class)
public interface TeacherClient {

    @GetMapping("/internal/courses/{courseId}/teacher-assigned")
    Result<CourseTeacherAssignedResponse> isCourseTeacherAssigned(@PathVariable("courseId") Long courseId);
}
