package com.yun.studentcourse.enrollment.mapper;

import com.yun.studentcourse.enrollment.entity.Enrollment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EnrollmentMapper {

    int insert(Enrollment enrollment);

    Enrollment findById(@Param("enrollmentId") Long enrollmentId);

    Enrollment findByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    List<Enrollment> findActiveByStudentId(@Param("studentId") Long studentId);

    List<Enrollment> findActiveByCourseId(@Param("courseId") Long courseId);

    List<Enrollment> findByStudentId(@Param("studentId") Long studentId);

    List<Enrollment> findPage(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            @Param("status") String status
    );

    long count(@Param("studentId") Long studentId, @Param("courseId") Long courseId, @Param("status") String status);

    int updateStatus(@Param("enrollmentId") Long enrollmentId, @Param("status") String status);
}
