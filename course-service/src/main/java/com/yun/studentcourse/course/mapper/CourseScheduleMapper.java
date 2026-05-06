package com.yun.studentcourse.course.mapper;

import com.yun.studentcourse.course.entity.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseScheduleMapper {

    int insert(CourseSchedule schedule);

    List<CourseSchedule> findByCourseId(@Param("courseId") Long courseId);
}
