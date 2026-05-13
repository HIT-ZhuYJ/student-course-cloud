package com.yun.studentcourse.teacher.service.impl;

import com.yun.studentcourse.common.BusinessException;
import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.Result;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.teacher.client.CourseClient;
import com.yun.studentcourse.teacher.client.StudentAccountClient;
import com.yun.studentcourse.teacher.client.dto.AccountStatusUpdateRequest;
import com.yun.studentcourse.teacher.client.dto.CourseScheduleResponse;
import com.yun.studentcourse.teacher.client.dto.TeacherAccountCreateRequest;
import com.yun.studentcourse.teacher.client.dto.TeacherAccountResponse;
import com.yun.studentcourse.teacher.dto.CourseTeacherAssignedResponse;
import com.yun.studentcourse.teacher.dto.TeacherCourseAssignmentResponse;
import com.yun.studentcourse.teacher.dto.TeacherCreateRequest;
import com.yun.studentcourse.teacher.dto.TeacherResponse;
import com.yun.studentcourse.teacher.dto.TeacherUpdateRequest;
import com.yun.studentcourse.teacher.entity.Teacher;
import com.yun.studentcourse.teacher.entity.TeacherCourseAssignment;
import com.yun.studentcourse.teacher.mapper.TeacherCourseAssignmentMapper;
import com.yun.studentcourse.teacher.mapper.TeacherMapper;
import com.yun.studentcourse.teacher.service.TeacherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TeacherServiceImpl implements TeacherService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";
    private static final String CANCELLED = "CANCELLED";

    private final TeacherMapper teacherMapper;
    private final TeacherCourseAssignmentMapper assignmentMapper;
    private final StudentAccountClient studentAccountClient;
    private final CourseClient courseClient;

    public TeacherServiceImpl(
            TeacherMapper teacherMapper,
            TeacherCourseAssignmentMapper assignmentMapper,
            StudentAccountClient studentAccountClient,
            CourseClient courseClient
    ) {
        this.teacherMapper = teacherMapper;
        this.assignmentMapper = assignmentMapper;
        this.studentAccountClient = studentAccountClient;
        this.courseClient = courseClient;
    }

    @Override
    @Transactional
    public TeacherResponse createTeacher(TeacherCreateRequest request) {
        if (teacherMapper.findByTeacherNo(request.getTeacherNo()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "teacherNo already exists");
        }
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : ACTIVE;
        validateTeacherStatus(status);

        Teacher teacher = new Teacher();
        teacher.setTeacherNo(request.getTeacherNo());
        teacher.setName(request.getName());
        teacher.setTitle(request.getTitle());
        teacher.setPhone(request.getPhone());
        teacher.setEmail(request.getEmail());
        teacher.setStatus(status);
        teacherMapper.insert(teacher);
        createTeacherAccount(request, teacher.getTeacherId(), status);
        return toTeacherResponse(teacherMapper.findById(teacher.getTeacherId()));
    }

    @Override
    @Transactional
    public TeacherResponse updateTeacher(Long teacherId, TeacherUpdateRequest request) {
        Teacher teacher = requireTeacher(teacherId);
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : teacher.getStatus();
        validateTeacherStatus(status);

        teacher.setName(request.getName());
        teacher.setTitle(request.getTitle());
        teacher.setPhone(request.getPhone());
        teacher.setEmail(request.getEmail());
        teacher.setStatus(status);
        teacherMapper.update(teacher);
        syncTeacherAccountStatus(teacherId, status);
        return toTeacherResponse(teacherMapper.findById(teacherId));
    }

    @Override
    @Transactional
    public void disableTeacher(Long teacherId) {
        requireTeacher(teacherId);
        teacherMapper.disable(teacherId);
        assignmentMapper.cancelActiveByTeacherId(teacherId);
        syncTeacherAccountStatus(teacherId, DISABLED);
    }

    @Override
    public TeacherResponse getTeacher(Long teacherId) {
        return toTeacherResponse(requireTeacher(teacherId));
    }

    @Override
    public PageResult<TeacherResponse> listTeachers(int pageNo, int pageSize, String keyword, String status) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        String normalizedStatus = normalizeStatusFilter(status);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        List<TeacherResponse> records = teacherMapper.findPage(
                        offset,
                        normalizedPageSize,
                        normalizeKeyword(keyword),
                        normalizedStatus
                )
                .stream()
                .map(this::toTeacherResponse)
                .toList();
        long total = teacherMapper.count(normalizeKeyword(keyword), normalizedStatus);
        return PageResult.of(records, total, normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public TeacherCourseAssignmentResponse assignCourse(Long teacherId, Long courseId) {
        Teacher teacher = requireTeacher(teacherId);
        if (!ACTIVE.equals(teacher.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "teacher is disabled");
        }
        validateTeacherScheduleConflict(teacherId, courseId);

        TeacherCourseAssignment activeAssignment = assignmentMapper.findActiveByCourseId(courseId);
        if (activeAssignment != null) {
            if (activeAssignment.getTeacherId().equals(teacherId)) {
                throw new BusinessException(ErrorCode.CONFLICT, "teacher already assigned to this course");
            }
            throw new BusinessException(ErrorCode.CONFLICT, "course already has an active teacher");
        }

        TeacherCourseAssignment existing = assignmentMapper.findByTeacherIdAndCourseId(teacherId, courseId);
        if (existing != null) {
            assignmentMapper.activate(existing.getId());
            return toAssignmentResponse(assignmentMapper.findByTeacherIdAndCourseId(teacherId, courseId));
        }

        TeacherCourseAssignment assignment = new TeacherCourseAssignment();
        assignment.setTeacherId(teacherId);
        assignment.setCourseId(courseId);
        assignment.setStatus(ACTIVE);
        assignmentMapper.insert(assignment);
        return toAssignmentResponse(assignmentMapper.findByTeacherIdAndCourseId(teacherId, courseId));
    }

    @Override
    @Transactional
    public void cancelCourseAssignment(Long teacherId, Long courseId) {
        requireTeacher(teacherId);
        int updated = assignmentMapper.cancel(teacherId, courseId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "active assignment not found");
        }
    }

    @Override
    public List<TeacherCourseAssignmentResponse> listTeacherCourses(Long teacherId) {
        requireTeacher(teacherId);
        return assignmentMapper.findActiveByTeacherId(teacherId)
                .stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Override
    public CourseTeacherAssignedResponse isCourseTeacherAssigned(Long courseId) {
        TeacherCourseAssignment assignment = assignmentMapper.findActiveByCourseId(courseId);
        if (assignment == null) {
            return new CourseTeacherAssignedResponse(courseId, false, null, null);
        }
        Teacher teacher = teacherMapper.findById(assignment.getTeacherId());
        boolean activeTeacher = teacher != null && ACTIVE.equals(teacher.getStatus());
        return new CourseTeacherAssignedResponse(
                courseId,
                activeTeacher,
                activeTeacher ? teacher.getTeacherId() : null,
                activeTeacher ? teacher.getName() : null
        );
    }

    private Teacher requireTeacher(Long teacherId) {
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "teacher not found");
        }
        return teacher;
    }

    private void validateTeacherStatus(String status) {
        if (!ACTIVE.equals(status) && !DISABLED.equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "teacher status must be ACTIVE or DISABLED");
        }
    }

    private String normalizeStatusFilter(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        validateTeacherStatus(status.trim());
        return status.trim();
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private void createTeacherAccount(TeacherCreateRequest request, Long teacherId, String status) {
        Result<TeacherAccountResponse> result = studentAccountClient.createTeacherAccount(
                new TeacherAccountCreateRequest(
                        request.getUsername(),
                        request.getPassword(),
                        teacherId,
                        status
                )
        );
        if (result == null) {
            throw new BusinessException(ErrorCode.REMOTE_SERVICE_ERROR, "teacher account creation returned empty response");
        }
        if (result.getCode() != ErrorCode.SUCCESS.getCode()) {
            throw new BusinessException(toErrorCode(result.getCode()), result.getMessage());
        }
    }

    private void syncTeacherAccountStatus(Long teacherId, String status) {
        Result<Void> result = studentAccountClient.updateTeacherAccountStatus(
                teacherId,
                new AccountStatusUpdateRequest(status)
        );
        if (result == null || result.getCode() != ErrorCode.SUCCESS.getCode()) {
            String message = result == null ? "teacher account status sync returned empty response" : result.getMessage();
            throw new BusinessException(result == null ? ErrorCode.REMOTE_SERVICE_ERROR : toErrorCode(result.getCode()), message);
        }
    }

    private void validateTeacherScheduleConflict(Long teacherId, Long targetCourseId) {
        List<CourseScheduleResponse> targetSchedules = requireRemoteData(
                courseClient.getSchedule(targetCourseId),
                "course schedule query failed"
        );
        for (TeacherCourseAssignment assignment : assignmentMapper.findActiveByTeacherId(teacherId)) {
            if (targetCourseId.equals(assignment.getCourseId())) {
                continue;
            }
            List<CourseScheduleResponse> existingSchedules = requireRemoteData(
                    courseClient.getSchedule(assignment.getCourseId()),
                    "teacher assigned course schedule query failed"
            );
            for (CourseScheduleResponse existing : existingSchedules) {
                for (CourseScheduleResponse target : targetSchedules) {
                    if (isScheduleConflict(existing, target)) {
                        throw new BusinessException(
                                ErrorCode.CONFLICT,
                                "teacher schedule conflicts with assigned courseId " + assignment.getCourseId()
                        );
                    }
                }
            }
        }
    }

    private boolean isScheduleConflict(CourseScheduleResponse left, CourseScheduleResponse right) {
        return left.getWeekday() == right.getWeekday()
                && left.getStartTime().isBefore(right.getEndTime())
                && right.getStartTime().isBefore(left.getEndTime());
    }

    private <T> T requireRemoteData(Result<T> result, String defaultMessage) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, defaultMessage);
        }
        if (result.getCode() != ErrorCode.SUCCESS.getCode()) {
            throw new BusinessException(toErrorCode(result.getCode()), StringUtils.hasText(result.getMessage()) ? result.getMessage() : defaultMessage);
        }
        return result.getData();
    }

    private ErrorCode toErrorCode(int code) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return ErrorCode.REMOTE_SERVICE_ERROR;
    }

    private TeacherResponse toTeacherResponse(Teacher teacher) {
        TeacherResponse response = new TeacherResponse();
        response.setTeacherId(teacher.getTeacherId());
        response.setTeacherNo(teacher.getTeacherNo());
        response.setName(teacher.getName());
        response.setTitle(teacher.getTitle());
        response.setPhone(teacher.getPhone());
        response.setEmail(teacher.getEmail());
        response.setStatus(teacher.getStatus());
        response.setCreateTime(teacher.getCreateTime());
        response.setUpdateTime(teacher.getUpdateTime());
        return response;
    }

    private TeacherCourseAssignmentResponse toAssignmentResponse(TeacherCourseAssignment assignment) {
        TeacherCourseAssignmentResponse response = new TeacherCourseAssignmentResponse();
        response.setId(assignment.getId());
        response.setTeacherId(assignment.getTeacherId());
        response.setCourseId(assignment.getCourseId());
        response.setStatus(assignment.getStatus());
        response.setCreateTime(assignment.getCreateTime());
        response.setUpdateTime(assignment.getUpdateTime());
        return response;
    }
}
