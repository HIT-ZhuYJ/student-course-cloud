package com.yun.studentcourse.enrollment.client.fallback;

import com.yun.studentcourse.common.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentClientFallbackTest {

    @Test
    void courseFallbackReportsServiceUnavailable() {
        CourseClientFallback fallback = new CourseClientFallback();

        assertThat(fallback.getCourse(10L).getCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE.getCode());
        assertThat(fallback.checkCapacity(10L).getMessage()).contains("course-service is unavailable");
    }

    @Test
    void studentFallbackReportsServiceUnavailable() {
        StudentClientFallback fallback = new StudentClientFallback();

        assertThat(fallback.getStudentStatus(1001L).getCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE.getCode());
        assertThat(fallback.getStudent(1001L).getMessage()).contains("student-service is unavailable");
    }

    @Test
    void teacherFallbackReportsServiceUnavailable() {
        TeacherClientFallback fallback = new TeacherClientFallback();

        assertThat(fallback.isCourseTeacherAssigned(10L).getCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE.getCode());
        assertThat(fallback.listTeacherCourses(3L).getMessage()).contains("teacher-service is unavailable");
    }
}
