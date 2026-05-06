package com.yun.studentcourse.student.mapper;

import com.yun.studentcourse.student.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentMapper {

    int insert(Student student);

    Student findById(@Param("studentId") Long studentId);

    Student findByStudentNo(@Param("studentNo") String studentNo);

    List<Student> findPage(@Param("offset") int offset, @Param("pageSize") int pageSize, @Param("keyword") String keyword);

    long count(@Param("keyword") String keyword);

    int update(Student student);
}
