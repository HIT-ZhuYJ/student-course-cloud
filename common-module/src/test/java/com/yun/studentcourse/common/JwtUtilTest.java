package com.yun.studentcourse.common;

import com.yun.studentcourse.common.dto.UserContext;
import com.yun.studentcourse.common.security.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "classroom-demo-secret-for-jwt-tests";

    @Test
    void generatedTokenCanBeParsedBackToUserContext() {
        UserContext userContext = new UserContext(1L, "alice", RoleEnum.STUDENT, 1001L);

        String token = JwtUtil.generateToken(SECRET, userContext, 60_000L);

        assertThat(JwtUtil.validateToken(SECRET, token)).isTrue();
        assertThat(JwtUtil.validateToken(SECRET, "Bearer " + token)).isTrue();

        UserContext parsed = JwtUtil.parseUserContext(SECRET, "Bearer " + token);
        assertThat(parsed.getUserId()).isEqualTo(1L);
        assertThat(parsed.getUsername()).isEqualTo("alice");
        assertThat(parsed.getRole()).isEqualTo(RoleEnum.STUDENT);
        assertThat(parsed.getRelatedId()).isEqualTo(1001L);
        assertThat(JwtUtil.getExpirationEpochMillis(SECRET, token)).isNotNull();
    }

    @Test
    void invalidTokenIsRejected() {
        assertThat(JwtUtil.validateToken(SECRET, "not-a-token")).isFalse();
        assertThat(JwtUtil.stripBearerPrefix("Bearer abc.def")).isEqualTo("abc.def");
    }
}
