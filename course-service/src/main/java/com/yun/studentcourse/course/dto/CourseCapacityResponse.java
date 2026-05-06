package com.yun.studentcourse.course.dto;

public class CourseCapacityResponse {

    private Long courseId;
    private boolean exists;
    private String status;
    private int capacity;
    private int selectedCount;
    private boolean selectable;
    private boolean hasCapacity;

    public CourseCapacityResponse() {
    }

    public CourseCapacityResponse(Long courseId, boolean exists, String status, int capacity, int selectedCount) {
        this.courseId = courseId;
        this.exists = exists;
        this.status = status;
        this.capacity = capacity;
        this.selectedCount = selectedCount;
        this.selectable = exists && "OPEN".equals(status);
        this.hasCapacity = exists && selectedCount < capacity;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getSelectedCount() {
        return selectedCount;
    }

    public void setSelectedCount(int selectedCount) {
        this.selectedCount = selectedCount;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public boolean isHasCapacity() {
        return hasCapacity;
    }

    public void setHasCapacity(boolean hasCapacity) {
        this.hasCapacity = hasCapacity;
    }
}
