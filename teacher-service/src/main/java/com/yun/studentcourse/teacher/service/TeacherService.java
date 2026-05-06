package com.yun.studentcourse.teacher.service;

import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.teacher.dto.CourseTeacherAssignedResponse;
import com.yun.studentcourse.teacher.dto.TeacherCourseAssignmentResponse;
import com.yun.studentcourse.teacher.dto.TeacherCreateRequest;
import com.yun.studentcourse.teacher.dto.TeacherResponse;
import com.yun.studentcourse.teacher.dto.TeacherUpdateRequest;

import java.util.List;

public interface TeacherService {

    TeacherResponse createTeacher(TeacherCreateRequest request);

    TeacherResponse updateTeacher(Long teacherId, TeacherUpdateRequest request);

    void disableTeacher(Long teacherId);

    TeacherResponse getTeacher(Long teacherId);

    PageResult<TeacherResponse> listTeachers(int pageNo, int pageSize, String keyword, String status);

    TeacherCourseAssignmentResponse assignCourse(Long teacherId, Long courseId);

    void cancelCourseAssignment(Long teacherId, Long courseId);

    List<TeacherCourseAssignmentResponse> listTeacherCourses(Long teacherId);

    CourseTeacherAssignedResponse isCourseTeacherAssigned(Long courseId);
}
