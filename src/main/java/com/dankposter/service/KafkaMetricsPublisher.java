package com.dankposter.service;

import com.dankposter.dto.KafkaMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMetricsPublisher {

    private final SseEmitterService sseEmitterService;

    public void publishProduced(String topic, String key, String payload) {
        KafkaMessageEvent event = new KafkaMessageEvent(
                topic, null, null, key, null, payload, "produced", Instant.now()
        );
        broadcastSafely(event);
    }

    public void publishConsumed(String topic, int partition, long offset, String key, long timestamp, String payload) {
        KafkaMessageEvent event = new KafkaMessageEvent(
                topic, partition, offset, key, Instant.ofEpochMilli(timestamp), payload, "consumed", Instant.now()
        );
        broadcastSafely(event);
    }

    public void publishDelivered(String topic, int partition, long offset, String key, long timestamp, String payload) {
        KafkaMessageEvent event = new KafkaMessageEvent(
                topic, partition, offset, key, Instant.ofEpochMilli(timestamp), payload, "delivered", Instant.now()
        );
        broadcastSafely(event);
    }

    public void publishFailed(String topic, int partition, long offset, String key, long timestamp, String payload) {
        KafkaMessageEvent event = new KafkaMessageEvent(
                topic, partition, offset, key, Instant.ofEpochMilli(timestamp), payload, "failed", Instant.now()
        );
        broadcastSafely(event);
    }

    private void broadcastSafely(KafkaMessageEvent event) {
        try {
            sseEmitterService.broadcast("kafka-metrics", event);
            log.debug("Broadcast kafka-metrics event: status={}, topic={}, key={}",
                    event.deliveryStatus(), event.topic(), event.key());
        } catch (Exception e) {
            log.error("Failed to broadcast kafka-metrics event: status={}, topic={}, key={}",
                    event.deliveryStatus(), event.topic(), event.key(), e);
        }
    }
}
