package com.yun.studentcourse.teacher.dto;

public class CourseTeacherAssignedResponse {

    private Long courseId;
    private boolean assigned;
    private Long teacherId;
    private String teacherName;

    public CourseTeacherAssignedResponse() {
    }

    public CourseTeacherAssignedResponse(Long courseId, boolean assigned, Long teacherId, String teacherName) {
        this.courseId = courseId;
        this.assigned = assigned;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public boolean isAssigned() {
        return assigned;
    }

    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }
}
