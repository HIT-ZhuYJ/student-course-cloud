package com.yun.studentcourse.teacher.mapper;

import com.yun.studentcourse.teacher.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeacherMapper {

    int insert(Teacher teacher);

    Teacher findById(@Param("teacherId") Long teacherId);

    Teacher findByTeacherNo(@Param("teacherNo") String teacherNo);

    List<Teacher> findPage(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("keyword") String keyword,
            @Param("status") String status
    );

    long count(@Param("keyword") String keyword, @Param("status") String status);

    int update(Teacher teacher);

    int disable(@Param("teacherId") Long teacherId);
}
