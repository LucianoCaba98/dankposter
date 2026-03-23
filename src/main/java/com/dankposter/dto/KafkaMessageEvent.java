package com.dankposter.dto;

import java.time.Instant;

/**
 * SSE payload representing a single Kafka message lifecycle event.
 *
 * @param topic          Kafka topic name
 * @param partition      partition number (null for "produced" events)
 * @param offset         message offset within partition (null for "produced" events)
 * @param key            message key (memeId as string)
 * @param timestamp      Kafka record timestamp (null for "produced" events)
 * @param payload        original JSON message body
 * @param deliveryStatus one of: produced, consumed, delivered, failed
 * @param capturedAt     server timestamp when event was captured
 */
public record KafkaMessageEvent(
    String topic,
    Integer partition,
    Long offset,
    String key,
    Instant timestamp,
    String payload,
    String deliveryStatus,
    Instant capturedAt
) {}
