package com.shitpostengine.dank.config;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "meme.queue.sqs.enabled", havingValue = "true")
public class SqsAutoConfiguration {

    private final SqsProperties sqsProperties;

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        log.info("Configuring SQS client for region: {}", sqsProperties.getRegion());
        return SqsAsyncClient.builder()
                .region(Region.of(sqsProperties.getRegion()))
                .build();
    }

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.newTemplate(sqsAsyncClient);
    }
}
