package com.yun.studentcourse.enrollment.client;

import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.enrollment.client.dto.CourseCapacityResponse;
import com.yun.studentcourse.enrollment.client.dto.CourseScheduleResponse;
import com.yun.studentcourse.enrollment.client.fallback.CourseClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(name = "course-service", contextId = "CourseClient", fallback = CourseClientFallback.class)
public interface CourseClient {

    @GetMapping("/internal/courses/{courseId}/check-capacity")
    Result<CourseCapacityResponse> checkCapacity(@PathVariable("courseId") Long courseId);

    @GetMapping("/internal/courses/{courseId}/schedule")
    Result<List<CourseScheduleResponse>> getSchedule(@PathVariable("courseId") Long courseId);

    @PostMapping("/internal/courses/{courseId}/increase-selected-count")
    Result<CourseCapacityResponse> increaseSelectedCount(@PathVariable("courseId") Long courseId);

    @PostMapping("/internal/courses/{courseId}/decrease-selected-count")
    Result<CourseCapacityResponse> decreaseSelectedCount(@PathVariable("courseId") Long courseId);
}
