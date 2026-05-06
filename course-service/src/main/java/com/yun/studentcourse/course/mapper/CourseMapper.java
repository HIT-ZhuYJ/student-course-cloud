package com.yun.studentcourse.course.mapper;

import com.yun.studentcourse.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseMapper {

    int insert(Course course);

    Course findById(@Param("courseId") Long courseId);

    Course findByCode(@Param("courseCode") String courseCode);

    List<Course> findPage(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("keyword") String keyword,
            @Param("status") String status
    );

    long count(@Param("keyword") String keyword, @Param("status") String status);

    int update(Course course);

    int disable(@Param("courseId") Long courseId);

    int increaseSelectedCount(@Param("courseId") Long courseId);

    int decreaseSelectedCount(@Param("courseId") Long courseId);
}
