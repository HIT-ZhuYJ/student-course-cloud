package com.yun.studentcourse.teacher.controller;

import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.teacher.dto.CourseTeacherAssignedResponse;
import com.yun.studentcourse.teacher.dto.TeacherResponse;
import com.yun.studentcourse.teacher.service.TeacherService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherControllerTest {

    private final TeacherService teacherService = mock(TeacherService.class);
    private final TeacherController controller = new TeacherController(teacherService);

    @Test
    void getTeacherReturnsServicePayloadWrappedInResult() {
        TeacherResponse teacher = new TeacherResponse();
        teacher.setTeacherId(3L);
        teacher.setName("Dr. Chen");
        when(teacherService.getTeacher(3L)).thenReturn(teacher);

        Result<TeacherResponse> result = controller.getTeacher(3L);

        assertThat(result.getCode()).isEqualTo(ErrorCode.SUCCESS.getCode());
        assertThat(result.getData().getName()).isEqualTo("Dr. Chen");
    }

    @Test
    void disableTeacherDelegatesAndReturnsSuccess() {
        Result<Void> result = controller.disableTeacher(3L);

        assertThat(result.getCode()).isEqualTo(ErrorCode.SUCCESS.getCode());
        verify(teacherService).disableTeacher(3L);
    }

    @Test
    void internalTeacherAssignmentCheckReturnsAssignmentState() {
        CourseTeacherAssignedResponse assigned = new CourseTeacherAssignedResponse(10L, true, 3L, "Dr. Chen");
        when(teacherService.isCourseTeacherAssigned(10L)).thenReturn(assigned);

        Result<CourseTeacherAssignedResponse> result = controller.isCourseTeacherAssigned(10L);

        assertThat(result.getData().isAssigned()).isTrue();
        assertThat(result.getData().getTeacherId()).isEqualTo(3L);
    }
}
