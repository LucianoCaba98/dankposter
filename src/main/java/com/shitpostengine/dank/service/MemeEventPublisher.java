package com.shitpostengine.dank.service;

import com.shitpostengine.dank.dto.MemeDto;
import com.shitpostengine.dank.model.Meme;
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
            MemeDto memeDto = MemeDto.fromEntity(meme);
            sseEmitterService.broadcast("ingestion", memeDto);
            log.debug("Published ingestion event for meme id={}", meme.getId());
        }
    }

    public void publishPosted(Meme meme) {
        MemeDto memeDto = MemeDto.fromEntity(meme);
        sseEmitterService.broadcast("posted", memeDto);
        log.debug("Published posted event for meme id={}", meme.getId());
    }
}
