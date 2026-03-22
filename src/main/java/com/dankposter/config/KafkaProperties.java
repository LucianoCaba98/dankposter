package com.dankposter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "meme.stream.kafka")
public class KafkaProperties {
    private boolean enabled = false;
    private String bootstrapServers = "localhost:9092";
    private String topic = "meme-delivery";
    private String consumerGroup = "dankposter-delivery";
}
