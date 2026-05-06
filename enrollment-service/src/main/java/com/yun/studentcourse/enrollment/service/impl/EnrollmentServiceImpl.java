package com.yun.studentcourse.enrollment.service.impl;

import com.yun.studentcourse.common.BusinessException;
import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.enrollment.client.CourseClient;
import com.yun.studentcourse.enrollment.client.StudentClient;
import com.yun.studentcourse.enrollment.client.TeacherClient;
import com.yun.studentcourse.enrollment.client.dto.CourseCapacityResponse;
import com.yun.studentcourse.enrollment.client.dto.CourseScheduleResponse;
import com.yun.studentcourse.enrollment.client.dto.CourseTeacherAssignedResponse;
import com.yun.studentcourse.enrollment.client.dto.StudentStatusResponse;
import com.yun.studentcourse.enrollment.dto.EnrollmentCreateRequest;
import com.yun.studentcourse.enrollment.dto.EnrollmentResponse;
import com.yun.studentcourse.enrollment.dto.TimetableResponse;
import com.yun.studentcourse.enrollment.entity.Enrollment;
import com.yun.studentcourse.enrollment.mapper.EnrollmentMapper;
import com.yun.studentcourse.enrollment.service.EnrollmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DROPPED = "DROPPED";

    private final EnrollmentMapper enrollmentMapper;
    private final StudentClient studentClient;
    private final CourseClient courseClient;
    private final TeacherClient teacherClient;

    public EnrollmentServiceImpl(
            EnrollmentMapper enrollmentMapper,
            StudentClient studentClient,
            CourseClient courseClient,
            TeacherClient teacherClient
    ) {
        this.enrollmentMapper = enrollmentMapper;
        this.studentClient = studentClient;
        this.courseClient = courseClient;
        this.teacherClient = teacherClient;
    }

    @Override
    @Transactional
    public EnrollmentResponse enroll(EnrollmentCreateRequest request) {
        validateStudent(request.getStudentId());
        validateCourse(request.getCourseId());
        validateTeacherAssigned(request.getCourseId());

        Enrollment existing = enrollmentMapper.findByStudentIdAndCourseId(request.getStudentId(), request.getCourseId());
        if (existing != null && ACTIVE.equals(existing.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "student has already selected this course");
        }

        List<CourseScheduleResponse> targetSchedules = getCourseSchedules(request.getCourseId());
        validateScheduleConflict(request.getStudentId(), request.getCourseId(), targetSchedules);

        Enrollment enrollment;
        if (existing == null) {
            enrollment = new Enrollment();
            enrollment.setStudentId(request.getStudentId());
            enrollment.setCourseId(request.getCourseId());
            enrollment.setStatus(ACTIVE);
            enrollmentMapper.insert(enrollment);
        } else {
            enrollmentMapper.updateStatus(existing.getEnrollmentId(), ACTIVE);
            enrollment = enrollmentMapper.findById(existing.getEnrollmentId());
        }

        /*
         * Classroom project consistency strategy:
         * enrollment owns the local enrollment row, course-service owns selected_count.
         * We do not use a distributed transaction framework here. The local transaction
         * writes/reactivates the enrollment, then calls course-service to increase the
         * counter. If that remote call fails, we throw an exception so the local
         * transaction rolls back and the caller gets a clear degradation/error message.
         */
        requireRemoteData(courseClient.increaseSelectedCount(request.getCourseId()), "increase course selected count failed");
        return toResponse(enrollmentMapper.findById(enrollment.getEnrollmentId()));
    }

    @Override
    @Transactional
    public EnrollmentResponse drop(Long enrollmentId, Long requesterStudentId, boolean admin) {
        Enrollment enrollment = requireEnrollment(enrollmentId);
        if (!admin && (requesterStudentId == null || !requesterStudentId.equals(enrollment.getStudentId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "students can only drop their own enrollments");
        }
        if (DROPPED.equals(enrollment.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "enrollment has already been dropped");
        }

        enrollmentMapper.updateStatus(enrollmentId, DROPPED);

        /*
         * Simplified compensation for the classroom demo: update the local row first,
         * then ask course-service to decrease selected_count. If the remote call fails,
         * throwing an exception rolls back the local status change. This keeps the demo
         * understandable without introducing Seata or another distributed transaction
         * framework.
         */
        requireRemoteData(courseClient.decreaseSelectedCount(enrollment.getCourseId()), "decrease course selected count failed");
        return toResponse(enrollmentMapper.findById(enrollmentId));
    }

    @Override
    public List<EnrollmentResponse> listStudentEnrollments(Long studentId) {
        return enrollmentMapper.findByStudentId(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TimetableResponse> getStudentTimetable(Long studentId) {
        validateStudent(studentId);
        List<TimetableResponse> timetable = new ArrayList<>();
        for (Enrollment enrollment : enrollmentMapper.findActiveByStudentId(studentId)) {
            for (CourseScheduleResponse schedule : getCourseSchedules(enrollment.getCourseId())) {
                timetable.add(toTimetableResponse(enrollment, schedule));
            }
        }
        return timetable;
    }

    @Override
    public PageResult<EnrollmentResponse> listEnrollments(int pageNo, int pageSize, Long studentId, Long courseId, String status) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        String normalizedStatus = normalizeStatus(status);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        List<EnrollmentResponse> records = enrollmentMapper.findPage(
                        offset,
                        normalizedPageSize,
                        studentId,
                        courseId,
                        normalizedStatus
                )
                .stream()
                .map(this::toResponse)
                .toList();
        long total = enrollmentMapper.count(studentId, courseId, normalizedStatus);
        return PageResult.of(records, total, normalizedPageNo, normalizedPageSize);
    }

    private void validateStudent(Long studentId) {
        StudentStatusResponse student = requireRemoteData(
                studentClient.getStudentStatus(studentId),
                "student status check failed"
        );
        if (!student.isExists()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "student not found");
        }
        if (!student.isActive()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "student is not active");
        }
    }

    private void validateCourse(Long courseId) {
        CourseCapacityResponse course = requireRemoteData(
                courseClient.checkCapacity(courseId),
                "course capacity check failed"
        );
        if (!course.isExists()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "course not found");
        }
        if (!course.isSelectable()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "course is not open for enrollment");
        }
        if (!course.isHasCapacity()) {
            throw new BusinessException(ErrorCode.CONFLICT, "course capacity is full");
        }
    }

    private void validateTeacherAssigned(Long courseId) {
        CourseTeacherAssignedResponse assigned = requireRemoteData(
                teacherClient.isCourseTeacherAssigned(courseId),
                "teacher assignment check failed"
        );
        if (!assigned.isAssigned()) {
            throw new BusinessException(ErrorCode.CONFLICT, "course has not been assigned a teacher");
        }
    }

    private List<CourseScheduleResponse> getCourseSchedules(Long courseId) {
        return requireRemoteData(courseClient.getSchedule(courseId), "course schedule query failed");
    }

    private void validateScheduleConflict(Long studentId, Long targetCourseId, List<CourseScheduleResponse> targetSchedules) {
        for (Enrollment activeEnrollment : enrollmentMapper.findActiveByStudentId(studentId)) {
            if (activeEnrollment.getCourseId().equals(targetCourseId)) {
                continue;
            }
            List<CourseScheduleResponse> existingSchedules = getCourseSchedules(activeEnrollment.getCourseId());
            for (CourseScheduleResponse existing : existingSchedules) {
                for (CourseScheduleResponse target : targetSchedules) {
                    if (isConflict(existing, target)) {
                        throw new BusinessException(
                                ErrorCode.CONFLICT,
                                "course schedule conflicts with selected courseId " + activeEnrollment.getCourseId()
                        );
                    }
                }
            }
        }
    }

    private boolean isConflict(CourseScheduleResponse left, CourseScheduleResponse right) {
        return left.getWeekday() == right.getWeekday()
                && left.getStartTime().isBefore(right.getEndTime())
                && right.getStartTime().isBefore(left.getEndTime());
    }

    private Enrollment requireEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentMapper.findById(enrollmentId);
        if (enrollment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "enrollment not found");
        }
        return enrollment;
    }

    private <T> T requireRemoteData(Result<T> result, String defaultMessage) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, defaultMessage);
        }
        if (result.getCode() != ErrorCode.SUCCESS.getCode()) {
            ErrorCode code = result.getCode() == ErrorCode.SERVICE_UNAVAILABLE.getCode()
                    ? ErrorCode.SERVICE_UNAVAILABLE
                    : ErrorCode.REMOTE_SERVICE_ERROR;
            String message = StringUtils.hasText(result.getMessage()) ? result.getMessage() : defaultMessage;
            throw new BusinessException(code, message);
        }
        return result.getData();
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim();
        if (!ACTIVE.equals(normalized) && !DROPPED.equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "enrollment status must be ACTIVE or DROPPED");
        }
        return normalized;
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        EnrollmentResponse response = new EnrollmentResponse();
        response.setEnrollmentId(enrollment.getEnrollmentId());
        response.setStudentId(enrollment.getStudentId());
        response.setCourseId(enrollment.getCourseId());
        response.setStatus(enrollment.getStatus());
        response.setCreateTime(enrollment.getCreateTime());
        response.setUpdateTime(enrollment.getUpdateTime());
        return response;
    }

    private TimetableResponse toTimetableResponse(Enrollment enrollment, CourseScheduleResponse schedule) {
        TimetableResponse response = new TimetableResponse();
        response.setEnrollmentId(enrollment.getEnrollmentId());
        response.setStudentId(enrollment.getStudentId());
        response.setCourseId(enrollment.getCourseId());
        response.setScheduleId(schedule.getScheduleId());
        response.setWeekday(schedule.getWeekday());
        response.setStartTime(schedule.getStartTime());
        response.setEndTime(schedule.getEndTime());
        response.setClassroom(schedule.getClassroom());
        return response;
    }
}
