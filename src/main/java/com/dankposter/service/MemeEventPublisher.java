package com.dankposter.service;

import com.dankposter.dto.MemeDto;
import com.dankposter.model.Meme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemeEventPublisher {

    private final SseEmitterService sseEmitterService;

    public void publishIngested(List<Meme> memes) {
        for (Meme meme : memes) {
            sseEmitterService.broadcast("ingestion", MemeDto.fromEntity(meme));
            log.debug("Published ingestion event for meme id={}", meme.getId());
        }
    }

    public void publishPosted(Meme meme) {
        sseEmitterService.broadcast("posted", MemeDto.fromEntity(meme));
        log.debug("Published posted event for meme id={}", meme.getId());
    }
}
