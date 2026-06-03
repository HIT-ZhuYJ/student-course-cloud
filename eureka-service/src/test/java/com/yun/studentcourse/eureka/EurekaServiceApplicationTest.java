package com.yun.studentcourse.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import static org.assertj.core.api.Assertions.assertThat;

class EurekaServiceApplicationTest {

    @Test
    void applicationDeclaresEurekaServerAndSpringBootApplication() {
        assertThat(EurekaServiceApplication.class).hasAnnotation(SpringBootApplication.class);
        assertThat(EurekaServiceApplication.class).hasAnnotation(EnableEurekaServer.class);
    }
}
