package com.dankposter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "groq")
public class GroqProperties {

    private Api api = new Api();
    private String model = "llama-3.3-70b-versatile";
    private int maxTokens = 1024;
    private boolean enabled = false;
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Api {
        private String key;
    }

    @Data
    public static class RateLimit {
        private int maxDailyCalls = 50;
    }
}
