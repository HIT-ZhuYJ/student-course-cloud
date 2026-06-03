package com.yun.studentcourse.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigServiceApplicationTest {

    @Test
    void applicationDeclaresConfigServerAndSpringBootApplication() {
        assertThat(ConfigServiceApplication.class).hasAnnotation(SpringBootApplication.class);
        assertThat(ConfigServiceApplication.class).hasAnnotation(EnableConfigServer.class);
    }
}
