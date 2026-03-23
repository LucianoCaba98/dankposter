package com.dankposter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "d3vd")
public class D3vdProperties {
    private int count = 20;
}
