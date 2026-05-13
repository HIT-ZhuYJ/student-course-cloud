package com.yun.studentcourse.student.service.impl;

import com.yun.studentcourse.common.BusinessException;
import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.RoleEnum;
import com.yun.studentcourse.common.dto.LoginRequest;
import com.yun.studentcourse.common.dto.LoginResponse;
import com.yun.studentcourse.common.dto.PageResult;
import com.yun.studentcourse.common.dto.RegisterRequest;
import com.yun.studentcourse.common.dto.UserContext;
import com.yun.studentcourse.common.security.JwtUtil;
import com.yun.studentcourse.student.dto.AccountStatusUpdateRequest;
import com.yun.studentcourse.student.dto.StudentResponse;
import com.yun.studentcourse.student.dto.StudentStatusResponse;
import com.yun.studentcourse.student.dto.StudentUpdateRequest;
import com.yun.studentcourse.student.dto.TeacherAccountCreateRequest;
import com.yun.studentcourse.student.dto.TeacherAccountResponse;
import com.yun.studentcourse.student.entity.Student;
import com.yun.studentcourse.student.entity.UserAccount;
import com.yun.studentcourse.student.mapper.StudentMapper;
import com.yun.studentcourse.student.mapper.UserAccountMapper;
import com.yun.studentcourse.student.service.StudentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class StudentServiceImpl implements StudentService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";

    private final StudentMapper studentMapper;
    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final String jwtSecret;
    private final long jwtExpirationMillis;

    public StudentServiceImpl(
            StudentMapper studentMapper,
            UserAccountMapper userAccountMapper,
            PasswordEncoder passwordEncoder,
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration-millis}") long jwtExpirationMillis
    ) {
        this.studentMapper = studentMapper;
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMillis = jwtExpirationMillis;
    }

    @Override
    @Transactional
    public StudentResponse register(RegisterRequest request) {
        if (studentMapper.findByStudentNo(request.getStudentNo()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "studentNo already exists");
        }
        if (userAccountMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "username already exists");
        }

        Student student = new Student();
        student.setStudentNo(request.getStudentNo());
        student.setName(request.getName());
        student.setMajor(request.getMajor());
        student.setGrade(request.getGrade());
        student.setPhone(request.getPhone());
        student.setEmail(request.getEmail());
        student.setStatus(ACTIVE);
        studentMapper.insert(student);

        UserAccount account = new UserAccount();
        account.setUsername(request.getUsername());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setRole(RoleEnum.STUDENT);
        account.setRelatedId(student.getStudentId());
        account.setStatus(ACTIVE);
        userAccountMapper.insert(account);

        return toResponse(studentMapper.findById(student.getStudentId()));
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        UserAccount account = userAccountMapper.findByUsername(request.getUsername());
        if (account == null || !passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "username or password is incorrect");
        }
        if (!ACTIVE.equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "account is disabled");
        }

        UserContext userContext = new UserContext(
                account.getId(),
                account.getUsername(),
                account.getRole(),
                account.getRelatedId()
        );
        String token = JwtUtil.generateToken(jwtSecret, userContext, jwtExpirationMillis);
        Long expiresAt = JwtUtil.getExpirationEpochMillis(jwtSecret, token);
        return new LoginResponse(token, account.getId(), account.getUsername(), account.getRole(), account.getRelatedId(), expiresAt);
    }

    @Override
    public StudentResponse getStudent(Long studentId) {
        return toResponse(requireStudent(studentId));
    }

    @Override
    @Transactional
    public StudentResponse updateStudent(Long studentId, StudentUpdateRequest request) {
        Student student = requireStudent(studentId);
        student.setName(request.getName());
        student.setMajor(request.getMajor());
        student.setGrade(request.getGrade());
        student.setPhone(request.getPhone());
        student.setEmail(request.getEmail());
        if (StringUtils.hasText(request.getStatus())) {
            validateStatus(request.getStatus());
            student.setStatus(request.getStatus());
        }
        studentMapper.update(student);
        userAccountMapper.updateStatusByRoleAndRelatedId(RoleEnum.STUDENT, studentId, student.getStatus());
        return toResponse(studentMapper.findById(studentId));
    }

    @Override
    public PageResult<StudentResponse> listStudents(int pageNo, int pageSize, String keyword) {
        int normalizedPageNo = Math.max(pageNo, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        List<StudentResponse> records = studentMapper.findPage(offset, normalizedPageSize, normalizeKeyword(keyword))
                .stream()
                .map(this::toResponse)
                .toList();
        long total = studentMapper.count(normalizeKeyword(keyword));
        return PageResult.of(records, total, normalizedPageNo, normalizedPageSize);
    }

    @Override
    public StudentStatusResponse getStudentStatus(Long studentId) {
        Student student = studentMapper.findById(studentId);
        if (student == null) {
            return new StudentStatusResponse(studentId, false, null, false);
        }
        return new StudentStatusResponse(studentId, true, student.getStatus(), ACTIVE.equals(student.getStatus()));
    }

    @Override
    @Transactional
    public TeacherAccountResponse createTeacherAccount(TeacherAccountCreateRequest request) {
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : ACTIVE;
        validateStatus(status);
        UserAccount existing = userAccountMapper.findByUsername(request.getUsername());
        if (existing != null) {
            if (RoleEnum.TEACHER.equals(existing.getRole()) && Objects.equals(existing.getRelatedId(), request.getTeacherId())) {
                return toTeacherAccountResponse(existing);
            }
            throw new BusinessException(ErrorCode.CONFLICT, "username already exists");
        }

        UserAccount account = new UserAccount();
        account.setUsername(request.getUsername());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setRole(RoleEnum.TEACHER);
        account.setRelatedId(request.getTeacherId());
        account.setStatus(status);
        userAccountMapper.insert(account);
        return toTeacherAccountResponse(account);
    }

    @Override
    @Transactional
    public void updateTeacherAccountStatus(Long teacherId, AccountStatusUpdateRequest request) {
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : ACTIVE;
        validateStatus(status);
        userAccountMapper.updateStatusByRoleAndRelatedId(RoleEnum.TEACHER, teacherId, status);
    }

    private Student requireStudent(Long studentId) {
        Student student = studentMapper.findById(studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "student not found");
        }
        return student;
    }

    private void validateStatus(String status) {
        if (!ACTIVE.equals(status) && !DISABLED.equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "student status must be ACTIVE or DISABLED");
        }
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private StudentResponse toResponse(Student student) {
        StudentResponse response = new StudentResponse();
        response.setStudentId(student.getStudentId());
        response.setStudentNo(student.getStudentNo());
        response.setName(student.getName());
        response.setMajor(student.getMajor());
        response.setGrade(student.getGrade());
        response.setPhone(student.getPhone());
        response.setEmail(student.getEmail());
        response.setStatus(student.getStatus());
        response.setCreateTime(student.getCreateTime());
        response.setUpdateTime(student.getUpdateTime());
        return response;
    }

    private TeacherAccountResponse toTeacherAccountResponse(UserAccount account) {
        TeacherAccountResponse response = new TeacherAccountResponse();
        response.setId(account.getId());
        response.setUsername(account.getUsername());
        response.setRole(account.getRole());
        response.setRelatedId(account.getRelatedId());
        response.setStatus(account.getStatus());
        return response;
    }
}
