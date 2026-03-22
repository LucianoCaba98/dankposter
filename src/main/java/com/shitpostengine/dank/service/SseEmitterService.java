package com.shitpostengine.dank.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseEmitterService {

    private static final long EMITTER_TIMEOUT = 1_800_000L; // 30 minutes

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitterService() {
        emitters.put("ingestion", new CopyOnWriteArrayList<>());
        emitters.put("posted", new CopyOnWriteArrayList<>());
    }

    public SseEmitter createEmitter(String channel) {
        CopyOnWriteArrayList<SseEmitter> channelEmitters = emitters.get(channel);
        if (channelEmitters == null) {
            throw new IllegalArgumentException("Unknown SSE channel: " + channel);
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);

        emitter.onCompletion(() -> {
            channelEmitters.remove(emitter);
            log.debug("SSE emitter completed and removed from channel '{}'", channel);
        });

        emitter.onTimeout(() -> {
            channelEmitters.remove(emitter);
            log.debug("SSE emitter timed out and removed from channel '{}'", channel);
        });

        emitter.onError(ex -> {
            channelEmitters.remove(emitter);
            log.warn("SSE emitter error on channel '{}': {}", channel, ex.getMessage());
        });

        channelEmitters.add(emitter);
        log.debug("New SSE emitter registered on channel '{}', total subscribers: {}", channel, channelEmitters.size());

        return emitter;
    }

    public void broadcast(String channel, Object data) {
        CopyOnWriteArrayList<SseEmitter> channelEmitters = emitters.get(channel);
        if (channelEmitters == null) {
            log.warn("Attempted to broadcast to unknown channel: {}", channel);
            return;
        }

        for (SseEmitter emitter : channelEmitters) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (IOException e) {
                channelEmitters.remove(emitter);
                log.debug("Removed dead SSE emitter from channel '{}': {}", channel, e.getMessage());
            }
        }
    }
}
