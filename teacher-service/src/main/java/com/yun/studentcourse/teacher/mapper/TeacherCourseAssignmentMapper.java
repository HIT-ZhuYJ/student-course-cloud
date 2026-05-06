package com.yun.studentcourse.teacher.mapper;

import com.yun.studentcourse.teacher.entity.TeacherCourseAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeacherCourseAssignmentMapper {

    int insert(TeacherCourseAssignment assignment);

    TeacherCourseAssignment findByTeacherIdAndCourseId(@Param("teacherId") Long teacherId, @Param("courseId") Long courseId);

    TeacherCourseAssignment findActiveByCourseId(@Param("courseId") Long courseId);

    List<TeacherCourseAssignment> findActiveByTeacherId(@Param("teacherId") Long teacherId);

    int activate(@Param("id") Long id);

    int cancel(@Param("teacherId") Long teacherId, @Param("courseId") Long courseId);

    int cancelActiveByTeacherId(@Param("teacherId") Long teacherId);
}
