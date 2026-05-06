package com.yun.studentcourse.course.service;

import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.course.dto.CourseCapacityResponse;
import com.yun.studentcourse.course.dto.CourseCreateRequest;
import com.yun.studentcourse.course.dto.CourseResponse;
import com.yun.studentcourse.course.dto.CourseScheduleRequest;
import com.yun.studentcourse.course.dto.CourseScheduleResponse;
import com.yun.studentcourse.course.dto.CourseUpdateRequest;

import java.util.List;

public interface CourseService {

    CourseResponse createCourse(CourseCreateRequest request);

    CourseResponse updateCourse(Long courseId, CourseUpdateRequest request);

    void disableCourse(Long courseId);

    CourseResponse getCourse(Long courseId);

    PageResult<CourseResponse> listCourses(int pageNo, int pageSize, String keyword, String status);

    CourseScheduleResponse addSchedule(Long courseId, CourseScheduleRequest request);

    List<CourseScheduleResponse> listSchedules(Long courseId);

    CourseCapacityResponse checkCapacity(Long courseId);

    CourseCapacityResponse increaseSelectedCount(Long courseId);

    CourseCapacityResponse decreaseSelectedCount(Long courseId);
}
