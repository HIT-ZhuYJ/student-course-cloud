package com.yun.studentcourse.course.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class CourseScheduleRequest {

    @Min(value = 1, message = "weekday must be between 1 and 7")
    @Max(value = 7, message = "weekday must be between 1 and 7")
    private int weekday;

    @NotNull(message = "startTime is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @NotNull(message = "endTime is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @NotBlank(message = "classroom is required")
    private String classroom;

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }
}
