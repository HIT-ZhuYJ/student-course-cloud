package com.yun.studentcourse.enrollment.controller;

import com.yun.studentcourse.common.BusinessException;
import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.enrollment.dto.EnrollmentCreateRequest;
import com.yun.studentcourse.enrollment.dto.EnrollmentResponse;
import com.yun.studentcourse.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnrollmentControllerTest {

    private final EnrollmentService enrollmentService = mock(EnrollmentService.class);
    private final EnrollmentController controller = new EnrollmentController(enrollmentService);

    @Test
    void studentCanEnrollForSelf() {
        EnrollmentCreateRequest request = new EnrollmentCreateRequest();
        request.setStudentId(1001L);
        request.setCourseId(10L);
        EnrollmentResponse response = new EnrollmentResponse();
        response.setEnrollmentId(99L);
        when(enrollmentService.enroll(request)).thenReturn(response);

        Result<EnrollmentResponse> result = controller.enroll("STUDENT", 1001L, request);

        assertThat(result.getCode()).isEqualTo(ErrorCode.SUCCESS.getCode());
        assertThat(result.getData().getEnrollmentId()).isEqualTo(99L);
        verify(enrollmentService).enroll(request);
    }

    @Test
    void studentCannotReadAnotherStudentsEnrollments() {
        assertThatThrownBy(() -> controller.listStudentEnrollments("STUDENT", 1001L, 2002L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("students can only operate their own enrollments");
    }

    @Test
    void adminCanReadAnyStudentEnrollments() {
        when(enrollmentService.listStudentEnrollments(2002L)).thenReturn(List.of(new EnrollmentResponse()));

        Result<List<EnrollmentResponse>> result = controller.listStudentEnrollments("ADMIN", null, 2002L);

        assertThat(result.getData()).hasSize(1);
        verify(enrollmentService).listStudentEnrollments(2002L);
    }
}
