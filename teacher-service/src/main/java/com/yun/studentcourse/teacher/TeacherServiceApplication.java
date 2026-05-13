package com.yun.studentcourse.teacher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;

@MapperScan("com.yun.studentcourse.teacher.mapper")
@EnableFeignClients
@SpringBootApplication
public class TeacherServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeacherServiceApplication.class, args);
    }
}
