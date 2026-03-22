package com.dankposter.service;

import com.dankposter.config.SqsProperties;
import com.dankposter.dto.MemeMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "meme.queue.sqs.enabled", havingValue = "true")
public class SqsProducerService {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;
    private final SqsProperties sqsProperties;

    public void send(MemeMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            sqsTemplate.send(sqsProperties.getQueueUrl(), json);
            log.debug("Sent meme message to SQS: sourceSpecificId={}, source={}",
                    message.sourceSpecificId(), message.sourceIdentifier());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize MemeMessage: sourceSpecificId={}", message.sourceSpecificId(), e);
        } catch (Exception e) {
            log.error("Failed to send MemeMessage to SQS: sourceSpecificId={}", message.sourceSpecificId(), e);
        }
    }
}
