package com.yun.studentcourse.course.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class CourseScheduleRequest {

    @Min(value = 1, message = "startWeek must be between 1 and 30")
    @Max(value = 30, message = "startWeek must be between 1 and 30")
    private int startWeek;

    @Min(value = 1, message = "endWeek must be between 1 and 30")
    @Max(value = 30, message = "endWeek must be between 1 and 30")
    private int endWeek;

    @NotBlank(message = "weekType is required")
    private String weekType;

    @Min(value = 1, message = "weekday must be between 1 and 7")
    @Max(value = 7, message = "weekday must be between 1 and 7")
    private int weekday;

    @Min(value = 1, message = "startSection must be between 1 and 12")
    @Max(value = 12, message = "startSection must be between 1 and 12")
    private int startSection;

    @Min(value = 1, message = "endSection must be between 1 and 12")
    @Max(value = 12, message = "endSection must be between 1 and 12")
    private int endSection;

    @NotNull(message = "startTime is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @NotNull(message = "endTime is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @NotBlank(message = "classroom is required")
    private String classroom;

    public int getStartWeek() {
        return startWeek;
    }

    public void setStartWeek(int startWeek) {
        this.startWeek = startWeek;
    }

    public int getEndWeek() {
        return endWeek;
    }

    public void setEndWeek(int endWeek) {
        this.endWeek = endWeek;
    }

    public String getWeekType() {
        return weekType;
    }

    public void setWeekType(String weekType) {
        this.weekType = weekType;
    }

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
    }

    public int getStartSection() {
        return startSection;
    }

    public void setStartSection(int startSection) {
        this.startSection = startSection;
    }

    public int getEndSection() {
        return endSection;
    }

    public void setEndSection(int endSection) {
        this.endSection = endSection;
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
