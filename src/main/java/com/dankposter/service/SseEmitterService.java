package com.dankposter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseEmitterService {

    private static final long EMITTER_TIMEOUT = 1_800_000L;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitterService() {
        emitters.put("ingestion", new CopyOnWriteArrayList<>());
        emitters.put("posted", new CopyOnWriteArrayList<>());
        emitters.put("kafka-metrics", new CopyOnWriteArrayList<>());
    }

    public SseEmitter createEmitter(String channel) {
        CopyOnWriteArrayList<SseEmitter> channelEmitters = emitters.get(channel);
        if (channelEmitters == null) {
            throw new IllegalArgumentException("Unknown SSE channel: " + channel);
        }
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        emitter.onCompletion(() -> channelEmitters.remove(emitter));
        emitter.onTimeout(() -> channelEmitters.remove(emitter));
        emitter.onError(ex -> channelEmitters.remove(emitter));
        channelEmitters.add(emitter);
        return emitter;
    }

    public void broadcast(String channel, Object data) {
        CopyOnWriteArrayList<SseEmitter> channelEmitters = emitters.get(channel);
        if (channelEmitters == null) return;
        for (SseEmitter emitter : channelEmitters) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (IOException e) {
                channelEmitters.remove(emitter);
            }
        }
    }
}
