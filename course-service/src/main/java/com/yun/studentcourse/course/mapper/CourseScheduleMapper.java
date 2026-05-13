package com.yun.studentcourse.course.mapper;

import com.yun.studentcourse.course.entity.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseScheduleMapper {

    int insert(CourseSchedule schedule);

    List<CourseSchedule> findByCourseId(@Param("courseId") Long courseId);

    List<CourseSchedule> findClassroomConflicts(
            @Param("classroom") String classroom,
            @Param("weekday") int weekday,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime
    );
}
