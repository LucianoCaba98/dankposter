package com.dankposter.controller;

import com.dankposter.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    @GetMapping("/ingestion")
    public SseEmitter streamIngestionEvents() {
        return sseEmitterService.createEmitter("ingestion");
    }

    @GetMapping("/posted")
    public SseEmitter streamPostedEvents() {
        return sseEmitterService.createEmitter("posted");
    }

    @GetMapping("/kafka-metrics")
    public SseEmitter streamKafkaMetrics() {
        return sseEmitterService.createEmitter("kafka-metrics");
    }
}
