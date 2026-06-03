package com.yun.studentcourse.gateway.filter;

import com.yun.studentcourse.common.ErrorCode;
import com.yun.studentcourse.common.RoleEnum;
import com.yun.studentcourse.common.dto.UserContext;
import com.yun.studentcourse.common.security.JwtUtil;
import com.yun.studentcourse.gateway.properties.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "gateway-test-secret";

    @Test
    void publicCourseQueryPassesWithoutToken() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProperties());
        AtomicBoolean called = new AtomicBoolean(false);
        GatewayFilterChain chain = exchange -> {
            called.set(true);
            return Mono.empty();
        };
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/courses").build()
        );

        filter.filter(exchange, chain).block();

        assertThat(called).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void protectedPathWithoutTokenReturnsUnauthorized() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProperties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/students/1001").build()
        );

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void studentTokenCanAccessOwnStudentResourceAndAddsHeaders() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProperties());
        String token = JwtUtil.generateToken(
                SECRET,
                new UserContext(1L, "student1", RoleEnum.STUDENT, 1001L),
                60_000L
        );
        AtomicBoolean called = new AtomicBoolean(false);
        GatewayFilterChain chain = exchange -> {
            called.set(true);
            assertThat(exchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("1");
            assertThat(exchange.getRequest().getHeaders().getFirst("X-Username")).isEqualTo("student1");
            assertThat(exchange.getRequest().getHeaders().getFirst("X-Role")).isEqualTo("STUDENT");
            assertThat(exchange.getRequest().getHeaders().getFirst("X-Related-Id")).isEqualTo("1001");
            return Mono.empty();
        };
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/students/1001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertThat(called).isTrue();
    }

    @Test
    void studentTokenCannotAccessAnotherStudentResource() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProperties());
        String token = JwtUtil.generateToken(
                SECRET,
                new UserContext(1L, "student1", RoleEnum.STUDENT, 1001L),
                60_000L
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/students/2002")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setPermitPaths(List.of("/api/auth/login", "/api/auth/register"));
        return properties;
    }
}
