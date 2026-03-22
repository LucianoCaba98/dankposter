package com.dankposter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "scheduling")
public class SchedulingProperties {
    private long fetchIntervalMs = 300000;
    private long postIntervalMs = 30000;
}
