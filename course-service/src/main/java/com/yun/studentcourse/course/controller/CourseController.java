package com.yun.studentcourse.course.controller;

import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.course.dto.CourseCapacityResponse;
import com.yun.studentcourse.course.dto.CourseCreateRequest;
import com.yun.studentcourse.course.dto.CourseResponse;
import com.yun.studentcourse.course.dto.CourseScheduleRequest;
import com.yun.studentcourse.course.dto.CourseScheduleResponse;
import com.yun.studentcourse.course.dto.CourseUpdateRequest;
import com.yun.studentcourse.course.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/courses")
    public Result<CourseResponse> createCourse(@Valid @RequestBody CourseCreateRequest request) {
        return Result.success(courseService.createCourse(request));
    }

    @PutMapping("/courses/{courseId}")
    public Result<CourseResponse> updateCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseUpdateRequest request
    ) {
        return Result.success(courseService.updateCourse(courseId, request));
    }

    @DeleteMapping("/courses/{courseId}")
    public Result<Void> disableCourse(@PathVariable Long courseId) {
        courseService.disableCourse(courseId);
        return Result.success();
    }

    @GetMapping("/courses/{courseId}")
    public Result<CourseResponse> getCourse(@PathVariable Long courseId) {
        return Result.success(courseService.getCourse(courseId));
    }

    @GetMapping("/courses")
    public Result<PageResult<CourseResponse>> listCourses(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return Result.success(courseService.listCourses(pageNo, pageSize, keyword, status));
    }

    @PostMapping("/courses/{courseId}/schedules")
    public Result<CourseScheduleResponse> addSchedule(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseScheduleRequest request
    ) {
        return Result.success(courseService.addSchedule(courseId, request));
    }

    @GetMapping("/courses/{courseId}/schedules")
    public Result<List<CourseScheduleResponse>> listSchedules(@PathVariable Long courseId) {
        return Result.success(courseService.listSchedules(courseId));
    }

    @GetMapping("/internal/courses/{courseId}/check-capacity")
    public Result<CourseCapacityResponse> checkCapacity(@PathVariable Long courseId) {
        return Result.success(courseService.checkCapacity(courseId));
    }

    @GetMapping("/internal/courses/{courseId}/schedule")
    public Result<List<CourseScheduleResponse>> getInternalSchedules(@PathVariable Long courseId) {
        return Result.success(courseService.listSchedules(courseId));
    }

    @PostMapping("/internal/courses/{courseId}/increase-selected-count")
    public Result<CourseCapacityResponse> increaseSelectedCount(@PathVariable Long courseId) {
        return Result.success(courseService.increaseSelectedCount(courseId));
    }

    @PostMapping("/internal/courses/{courseId}/decrease-selected-count")
    public Result<CourseCapacityResponse> decreaseSelectedCount(@PathVariable Long courseId) {
        return Result.success(courseService.decreaseSelectedCount(courseId));
    }
}
