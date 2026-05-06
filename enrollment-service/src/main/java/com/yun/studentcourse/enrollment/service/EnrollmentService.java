package com.yun.studentcourse.enrollment.service;

import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.enrollment.dto.EnrollmentCreateRequest;
import com.yun.studentcourse.enrollment.dto.EnrollmentResponse;
import com.yun.studentcourse.enrollment.dto.TimetableResponse;

import java.util.List;

public interface EnrollmentService {

    EnrollmentResponse enroll(EnrollmentCreateRequest request);

    EnrollmentResponse drop(Long enrollmentId);

    List<EnrollmentResponse> listStudentEnrollments(Long studentId);

    List<TimetableResponse> getStudentTimetable(Long studentId);

    PageResult<EnrollmentResponse> listEnrollments(int pageNo, int pageSize, Long studentId, Long courseId, String status);
}
