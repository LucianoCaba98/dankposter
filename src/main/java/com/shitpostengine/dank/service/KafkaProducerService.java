package com.shitpostengine.dank.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shitpostengine.dank.config.KafkaProperties;
import com.shitpostengine.dank.dto.MemeDeliveryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka producer service for publishing meme delivery events.
 * Retry (3 attempts, exponential backoff) is handled by the Kafka producer config
 * in {@link com.shitpostengine.dank.config.KafkaAutoConfiguration}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "meme.stream.kafka.enabled", havingValue = "true")
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaProperties kafkaProperties;

    /**
     * Publishes a meme delivery event to the configured Kafka topic.
     *
     * @param event the delivery event to publish
     */
    public void publishDeliveryEvent(MemeDeliveryEvent event) {
        String topic = kafkaProperties.getTopic();
        String key = String.valueOf(event.memeId());

        try {
            String value = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, value)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish delivery event for memeId={}: {}",
                                    event.memeId(), ex.getMessage());
                        } else {
                            log.debug("Published delivery event for memeId={} to topic={}",
                                    event.memeId(), topic);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize delivery event for memeId={}: {}",
                    event.memeId(), e.getMessage());
        }
    }
}
