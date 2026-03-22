package com.dankposter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "meme.queue.sqs")
public class SqsProperties {
    private boolean enabled = false;
    private String queueUrl;
    private String dlqUrl;
    private String region = "us-east-1";
    private Duration pollInterval = Duration.ofSeconds(10);
}
