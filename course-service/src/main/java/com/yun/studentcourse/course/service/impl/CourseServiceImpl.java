package com.yun.studentcourse.course.service.impl;

import com.yun.studentcourse.common.BusinessException;
import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.course.dto.CourseCapacityResponse;
import com.yun.studentcourse.course.dto.CourseCreateRequest;
import com.yun.studentcourse.course.dto.CourseResponse;
import com.yun.studentcourse.course.dto.CourseScheduleRequest;
import com.yun.studentcourse.course.dto.CourseScheduleResponse;
import com.yun.studentcourse.course.dto.CourseUpdateRequest;
import com.yun.studentcourse.course.entity.Course;
import com.yun.studentcourse.course.entity.CourseSchedule;
import com.yun.studentcourse.course.mapper.CourseMapper;
import com.yun.studentcourse.course.mapper.CourseScheduleMapper;
import com.yun.studentcourse.course.service.CourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private static final String OPEN = "OPEN";
    private static final String CLOSED = "CLOSED";
    private static final String DISABLED = "DISABLED";
    private static final String WEEK_ALL = "ALL";
    private static final String WEEK_ODD = "ODD";
    private static final String WEEK_EVEN = "EVEN";

    private final CourseMapper courseMapper;
    private final CourseScheduleMapper courseScheduleMapper;

    public CourseServiceImpl(CourseMapper courseMapper, CourseScheduleMapper courseScheduleMapper) {
        this.courseMapper = courseMapper;
        this.courseScheduleMapper = courseScheduleMapper;
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        if (courseMapper.findByCode(request.getCourseCode()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "courseCode already exists");
        }
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : OPEN;
        validateStatus(status);

        Course course = new Course();
        course.setCourseCode(request.getCourseCode());
        course.setCourseName(request.getCourseName());
        course.setCredit(request.getCredit());
        course.setCapacity(request.getCapacity());
        course.setSelectedCount(0);
        course.setStatus(status);
        course.setDescription(request.getDescription());
        courseMapper.insert(course);
        createSchedule(course.getCourseId(), request.getSchedule());
        return toResponse(courseMapper.findById(course.getCourseId()));
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(Long courseId, CourseUpdateRequest request) {
        Course course = requireCourse(courseId);
        if (request.getCapacity() < course.getSelectedCount()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "capacity cannot be less than selectedCount");
        }
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : course.getStatus();
        validateStatus(status);

        course.setCourseName(request.getCourseName());
        course.setCredit(request.getCredit());
        course.setCapacity(request.getCapacity());
        course.setStatus(status);
        course.setDescription(request.getDescription());
        courseMapper.update(course);
        return toResponse(courseMapper.findById(courseId));
    }

    @Override
    @Transactional
    public void disableCourse(Long courseId) {
        requireCourse(courseId);
        courseMapper.disable(courseId);
    }

    @Override
    public CourseResponse getCourse(Long courseId) {
        return toResponse(requireCourse(courseId));
    }

    @Override
    public PageResult<CourseResponse> listCourses(int pageNo, int pageSize, String keyword, String status) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        String normalizedStatus = normalizeStatusFilter(status);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        List<CourseResponse> records = courseMapper.findPage(
                        offset,
                        normalizedPageSize,
                        normalizeKeyword(keyword),
                        normalizedStatus
                )
                .stream()
                .map(this::toResponse)
                .toList();
        long total = courseMapper.count(normalizeKeyword(keyword), normalizedStatus);
        return PageResult.of(records, total, normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public CourseScheduleResponse addSchedule(Long courseId, CourseScheduleRequest request) {
        requireCourse(courseId);
        return createSchedule(courseId, request);
    }

    private CourseScheduleResponse createSchedule(Long courseId, CourseScheduleRequest request) {
        validateWeekRange(request.getStartWeek(), request.getEndWeek());
        String weekType = normalizeWeekType(request.getWeekType());
        if (request.getStartSection() > request.getEndSection()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "startSection must be less than or equal to endSection");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "startTime must be before endTime");
        }
        String classroom = request.getClassroom().trim();
        List<CourseSchedule> classroomConflicts = courseScheduleMapper.findClassroomConflicts(
                classroom,
                request.getStartWeek(),
                request.getEndWeek(),
                request.getWeekday(),
                request.getStartTime(),
                request.getEndTime()
        ).stream().filter(existing -> weeksOverlap(
                existing.getStartWeek(),
                existing.getEndWeek(),
                existing.getWeekType(),
                request.getStartWeek(),
                request.getEndWeek(),
                weekType
        )).toList();
        if (!classroomConflicts.isEmpty()) {
            CourseSchedule conflict = classroomConflicts.get(0);
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "classroom schedule conflicts with courseId " + conflict.getCourseId()
            );
        }

        CourseSchedule schedule = new CourseSchedule();
        schedule.setCourseId(courseId);
        schedule.setStartWeek(request.getStartWeek());
        schedule.setEndWeek(request.getEndWeek());
        schedule.setWeekType(weekType);
        schedule.setWeekday(request.getWeekday());
        schedule.setStartSection(request.getStartSection());
        schedule.setEndSection(request.getEndSection());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setClassroom(classroom);
        courseScheduleMapper.insert(schedule);
        return toScheduleResponse(schedule);
    }

    @Override
    public List<CourseScheduleResponse> listSchedules(Long courseId) {
        requireCourse(courseId);
        return courseScheduleMapper.findByCourseId(courseId)
                .stream()
                .map(this::toScheduleResponse)
                .toList();
    }

    @Override
    public CourseCapacityResponse checkCapacity(Long courseId) {
        Course course = courseMapper.findById(courseId);
        if (course == null) {
            return new CourseCapacityResponse(courseId, false, null, 0, 0);
        }
        return toCapacityResponse(course);
    }

    @Override
    @Transactional
    public CourseCapacityResponse increaseSelectedCount(Long courseId) {
        requireCourse(courseId);
        int updated = courseMapper.increaseSelectedCount(courseId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "course is not open or capacity is full");
        }
        return toCapacityResponse(requireCourse(courseId));
    }

    @Override
    @Transactional
    public CourseCapacityResponse decreaseSelectedCount(Long courseId) {
        requireCourse(courseId);
        int updated = courseMapper.decreaseSelectedCount(courseId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "selectedCount is already zero");
        }
        return toCapacityResponse(requireCourse(courseId));
    }

    private Course requireCourse(Long courseId) {
        Course course = courseMapper.findById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "course not found");
        }
        return course;
    }

    private void validateStatus(String status) {
        if (!OPEN.equals(status) && !CLOSED.equals(status) && !DISABLED.equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "course status must be OPEN, CLOSED or DISABLED");
        }
    }

    private String normalizeStatusFilter(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        validateStatus(status.trim());
        return status.trim();
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private void validateWeekRange(int startWeek, int endWeek) {
        if (startWeek < 1 || endWeek > 30 || startWeek > endWeek) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "week range must be between 1 and 30 and startWeek must be <= endWeek");
        }
    }

    private String normalizeWeekType(String weekType) {
        String normalized = StringUtils.hasText(weekType) ? weekType.trim().toUpperCase() : WEEK_ALL;
        if (!WEEK_ALL.equals(normalized) && !WEEK_ODD.equals(normalized) && !WEEK_EVEN.equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "weekType must be ALL, ODD or EVEN");
        }
        return normalized;
    }

    private boolean weeksOverlap(int leftStart, int leftEnd, String leftType, int rightStart, int rightEnd, String rightType) {
        int start = Math.max(leftStart, rightStart);
        int end = Math.min(leftEnd, rightEnd);
        if (start > end) {
            return false;
        }
        for (int week = start; week <= end; week++) {
            if (matchesWeekType(week, leftType) && matchesWeekType(week, rightType)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesWeekType(int week, String weekType) {
        String normalized = normalizeWeekType(weekType);
        return WEEK_ALL.equals(normalized)
                || (WEEK_ODD.equals(normalized) && week % 2 == 1)
                || (WEEK_EVEN.equals(normalized) && week % 2 == 0);
    }

    private CourseCapacityResponse toCapacityResponse(Course course) {
        return new CourseCapacityResponse(
                course.getCourseId(),
                true,
                course.getStatus(),
                course.getCapacity(),
                course.getSelectedCount()
        );
    }

    private CourseResponse toResponse(Course course) {
        CourseResponse response = new CourseResponse();
        response.setCourseId(course.getCourseId());
        response.setCourseCode(course.getCourseCode());
        response.setCourseName(course.getCourseName());
        response.setCredit(course.getCredit());
        response.setCapacity(course.getCapacity());
        response.setSelectedCount(course.getSelectedCount());
        response.setStatus(course.getStatus());
        response.setDescription(course.getDescription());
        response.setCreateTime(course.getCreateTime());
        response.setUpdateTime(course.getUpdateTime());
        return response;
    }

    private CourseScheduleResponse toScheduleResponse(CourseSchedule schedule) {
        CourseScheduleResponse response = new CourseScheduleResponse();
        response.setScheduleId(schedule.getScheduleId());
        response.setCourseId(schedule.getCourseId());
        response.setStartWeek(schedule.getStartWeek());
        response.setEndWeek(schedule.getEndWeek());
        response.setWeekType(schedule.getWeekType());
        response.setWeekday(schedule.getWeekday());
        response.setStartSection(schedule.getStartSection());
        response.setEndSection(schedule.getEndSection());
        response.setStartTime(schedule.getStartTime());
        response.setEndTime(schedule.getEndTime());
        response.setClassroom(schedule.getClassroom());
        response.setCreateTime(schedule.getCreateTime());
        response.setUpdateTime(schedule.getUpdateTime());
        return response;
    }
}
