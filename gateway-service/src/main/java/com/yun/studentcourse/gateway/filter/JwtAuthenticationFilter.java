package com.yun.studentcourse.gateway.filter;

import com.yun.studentcourse.common.RoleEnum;
import com.yun.studentcourse.common.dto.UserContext;
import com.yun.studentcourse.common.security.JwtUtil;
import com.yun.studentcourse.gateway.properties.JwtProperties;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties jwtProperties;

    public JwtAuthenticationFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        if (HttpMethod.OPTIONS.equals(method) || isPermitPath(path) || isPublicCourseQuery(method, path)) {
            return chain.filter(exchange);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "missing Authorization Bearer token");
        }

        UserContext userContext;
        try {
            userContext = JwtUtil.parseUserContext(jwtProperties.getSecret(), authorization);
        } catch (JwtException | IllegalArgumentException ex) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "invalid or expired token");
        }

        if (!hasPermission(method, path, userContext)) {
            return writeError(exchange, HttpStatus.FORBIDDEN, "permission denied");
        }

        ServerHttpRequest.Builder requestBuilder = request.mutate()
                .header("X-User-Id", String.valueOf(userContext.getUserId()))
                .header("X-Username", userContext.getUsername())
                .header("X-Role", userContext.getRole().name());
        if (userContext.getRelatedId() != null) {
            requestBuilder.header("X-Related-Id", String.valueOf(userContext.getRelatedId()));
        }
        ServerHttpRequest mutatedRequest = requestBuilder.build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPermitPath(String path) {
        return jwtProperties.getPermitPaths().stream().anyMatch(path::equals);
    }

    private boolean isPublicCourseQuery(HttpMethod method, String path) {
        /*
         * Classroom demo choice: course browsing is public so students can view
         * the course catalog before logging in. Course mutations are still admin-only.
         */
        return HttpMethod.GET.equals(method) && path.startsWith("/api/courses");
    }

    private boolean hasPermission(HttpMethod method, String path, UserContext userContext) {
        RoleEnum role = userContext.getRole();
        if (RoleEnum.ADMIN.equals(role)) {
            return true;
        }

        if (path.startsWith("/api/courses")) {
            return false;
        }

        if (path.startsWith("/api/students")) {
            return hasStudentPermission(method, path, userContext);
        }

        if (path.startsWith("/api/teachers")) {
            return hasTeacherPermission(method, path, userContext);
        }

        if (path.startsWith("/api/enrollments")) {
            return hasEnrollmentPermission(method, path, userContext);
        }

        return false;
    }

    private boolean hasStudentPermission(HttpMethod method, String path, UserContext userContext) {
        if (!RoleEnum.STUDENT.equals(userContext.getRole())) {
            return false;
        }
        Long pathStudentId = pathId(path, "/api/students/");
        return pathStudentId != null
                && pathStudentId.equals(userContext.getRelatedId())
                && (HttpMethod.GET.equals(method) || HttpMethod.PUT.equals(method));
    }

    private boolean hasTeacherPermission(HttpMethod method, String path, UserContext userContext) {
        if (!RoleEnum.TEACHER.equals(userContext.getRole()) || !HttpMethod.GET.equals(method)) {
            return false;
        }
        String prefix = "/api/teachers/";
        if (!path.startsWith(prefix) || !path.endsWith("/courses")) {
            return false;
        }
        String middle = path.substring(prefix.length(), path.length() - "/courses".length());
        Long teacherId = parseLong(middle);
        return teacherId != null && teacherId.equals(userContext.getRelatedId());
    }

    private boolean hasEnrollmentPermission(HttpMethod method, String path, UserContext userContext) {
        if (!RoleEnum.STUDENT.equals(userContext.getRole())) {
            return false;
        }
        if (HttpMethod.POST.equals(method) && "/api/enrollments".equals(path)) {
            return true;
        }
        if (HttpMethod.DELETE.equals(method) && pathId(path, "/api/enrollments/") != null) {
            return true;
        }
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/enrollments/students/")) {
            Long studentId = pathId(path, "/api/enrollments/students/");
            return studentId != null && studentId.equals(userContext.getRelatedId());
        }
        return false;
    }

    private Long pathId(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return null;
        }
        String tail = path.substring(prefix.length());
        int slashIndex = tail.indexOf('/');
        String rawId = slashIndex >= 0 ? tail.substring(0, slashIndex) : tail;
        return parseLong(rawId);
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"code":%d,"message":"%s","data":null,"timestamp":%d}
                """.formatted(status.value(), message, System.currentTimeMillis()).trim();
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
