package com.yun.studentcourse.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void successResultContainsSuccessCodeAndPayload() {
        Result<String> result = Result.success("ok");

        assertThat(result.getCode()).isEqualTo(ErrorCode.SUCCESS.getCode());
        assertThat(result.getMessage()).isEqualTo(ErrorCode.SUCCESS.getMessage());
        assertThat(result.getData()).isEqualTo("ok");
        assertThat(result.getTimestamp()).isPositive();
    }

    @Test
    void failureResultCanOverrideMessage() {
        Result<Object> result = Result.fail(ErrorCode.FORBIDDEN, "permission denied");

        assertThat(result.getCode()).isEqualTo(ErrorCode.FORBIDDEN.getCode());
        assertThat(result.getMessage()).isEqualTo("permission denied");
        assertThat(result.getData()).isNull();
    }
}
