package com.yun.studentcourse.gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private List<String> permitPaths = new ArrayList<>();

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getPermitPaths() {
        return permitPaths;
    }

    public void setPermitPaths(List<String> permitPaths) {
        this.permitPaths = permitPaths;
    }
}
