package com.yun.studentcourse.teacher.client;

import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.teacher.client.dto.CourseScheduleResponse;
import com.yun.studentcourse.teacher.client.fallback.CourseClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "course-service", contextId = "TeacherCourseClient", fallback = CourseClientFallback.class)
public interface CourseClient {

    @GetMapping("/internal/courses/{courseId}/schedule")
    Result<List<CourseScheduleResponse>> getSchedule(@PathVariable("courseId") Long courseId);
}
