package com.dankposter.service;

import com.dankposter.config.KafkaProperties;
import com.dankposter.dto.MemeDeliveryEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaProperties kafkaProperties;
    private final KafkaMetricsPublisher kafkaMetricsPublisher;

    public void publishDeliveryEvent(MemeDeliveryEvent event) {
        String topic = kafkaProperties.getTopic();
        String key = String.valueOf(event.memeId());
        try {
            String value = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, value)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish delivery event for memeId={}: {}", event.memeId(), ex.getMessage());
                        } else {
                            log.debug("Published delivery event for memeId={} to topic={}", event.memeId(), topic);
                            try {
                                kafkaMetricsPublisher.publishProduced(topic, key, value);
                            } catch (Exception e) {
                                log.error("Failed to publish kafka metrics for memeId={}: {}", event.memeId(), e.getMessage());
                            }
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize delivery event for memeId={}: {}", event.memeId(), e.getMessage());
        }
    }
}
