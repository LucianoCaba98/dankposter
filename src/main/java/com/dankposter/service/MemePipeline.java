package com.dankposter.service;

import com.dankposter.model.MemeStatus;
import com.dankposter.repository.MemeRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class MemePipeline {

    private final MemeFetchService memeFetchService;
    private final DiscordPosterService discordPosterService;
    private final MemeRepository memeRepository;
    private final MemeEventPublisher memeEventPublisher;

    public MemePipeline(MemeFetchService memeFetchService,
                        DiscordPosterService discordPosterService,
                        MemeRepository memeRepository,
                        MemeEventPublisher memeEventPublisher) {
        this.memeFetchService = memeFetchService;
        this.discordPosterService = discordPosterService;
        this.memeRepository = memeRepository;
        this.memeEventPublisher = memeEventPublisher;
    }

    @PostConstruct
    public void start() {
        Flux.interval(Duration.ZERO, Duration.ofMinutes(5))
                .flatMap(tick -> memeFetchService.fetch())
                .concatMap(meme ->
                        Mono.fromCallable(() -> memeRepository.save(meme))
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnNext(saved -> memeEventPublisher.publishIngested(List.of(saved)))
                                .onErrorResume(DataIntegrityViolationException.class, e -> {
                                    log.debug("Duplicate!: {}", meme.getExternalId());
                                    return Mono.empty();
                                })
                )
                .filter(meme -> meme.getStatus() == MemeStatus.FETCHED)
                .delayElements(Duration.ofSeconds(30))
                .concatMap(meme ->
                        discordPosterService.post(meme)
                                .flatMap(posted -> {
                                    posted.setStatus(MemeStatus.POSTED);
                                    return Mono.fromCallable(() -> memeRepository.save(posted))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .doOnNext(memeEventPublisher::publishPosted);
                                })
                                .onErrorResume(e -> {
                                    meme.setStatus(MemeStatus.FAILED);
                                    log.error("Failed posting meme {}", meme.getExternalId(), e);
                                    return Mono.fromCallable(() -> memeRepository.save(meme))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .then(Mono.empty());
                                })
                )
                .subscribe(
                        success -> log.info("Posted meme {}", success.getExternalId()),
                        error -> log.error("Pipeline error", error)
                );
    }
}
