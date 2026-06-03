package com.yun.studentcourse.course.controller;

import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.course.dto.CourseCapacityResponse;
import com.yun.studentcourse.course.dto.CourseResponse;
import com.yun.studentcourse.course.service.CourseService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseControllerTest {

    private final CourseService courseService = mock(CourseService.class);
    private final CourseController controller = new CourseController(courseService);

    @Test
    void getCourseReturnsServicePayloadWrappedInResult() {
        CourseResponse course = new CourseResponse();
        course.setCourseId(10L);
        course.setCourseName("Cloud Native");
        when(courseService.getCourse(10L)).thenReturn(course);

        Result<CourseResponse> result = controller.getCourse(10L);

        assertThat(result.getCode()).isEqualTo(ErrorCode.SUCCESS.getCode());
        assertThat(result.getData().getCourseName()).isEqualTo("Cloud Native");
    }

    @Test
    void listCoursesPassesQueryParametersToService() {
        PageResult<CourseResponse> page = PageResult.of(List.of(new CourseResponse()), 1, 2, 5);
        when(courseService.listCourses(2, 5, "cloud", "OPEN")).thenReturn(page);

        Result<PageResult<CourseResponse>> result = controller.listCourses(2, 5, "cloud", "OPEN");

        assertThat(result.getData().getTotal()).isEqualTo(1);
        verify(courseService).listCourses(2, 5, "cloud", "OPEN");
    }

    @Test
    void internalCapacityCheckReturnsCapacityStatus() {
        CourseCapacityResponse capacity = new CourseCapacityResponse(10L, true, "OPEN", 30, 20);
        when(courseService.checkCapacity(10L)).thenReturn(capacity);

        Result<CourseCapacityResponse> result = controller.checkCapacity(10L);

        assertThat(result.getData().isSelectable()).isTrue();
        assertThat(result.getData().isHasCapacity()).isTrue();
    }
}
