package com.shitpostengine.dank.service;

import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.model.MemeStatus;
import com.shitpostengine.dank.repository.MemeRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Service
@Slf4j
public class MemeFetchPipeline {

    private final RedditFetcherService redditFetcherService;
    private final DiscordPosterService discordPosterService;
    private final MemeRepository memeRepository;

    public MemeFetchPipeline(RedditFetcherService redditFetcherService, DiscordPosterService discordPosterService, MemeRepository memeRepository) {
        this.redditFetcherService = redditFetcherService;
        this.discordPosterService = discordPosterService;
        this.memeRepository = memeRepository;
    }

    @PostConstruct
    public void start() {
        Flux.interval(Duration.ofSeconds(30))
                .flatMap(tick -> redditFetcherService.fetch())
                .flatMap(meme ->
                        Mono.fromCallable(() -> memeRepository.save(meme))
                                .subscribeOn(Schedulers.boundedElastic())
                                .onErrorResume(DataIntegrityViolationException.class, e -> {
                            log.debug("Duplicate!: {}", meme.getRedditId());
                            return Mono.empty();
                        })
                )
                .filter(meme -> meme.getStatus() == MemeStatus.FETCHED)
                .delayElements(Duration.ofSeconds(2))
                .concatMap( meme ->
                        discordPosterService.post(meme)
                                .flatMap(posted -> {
                                    posted.setStatus(MemeStatus.POSTED);
                                    return Mono.fromCallable(() -> memeRepository.save(posted))
                                            .subscribeOn(Schedulers.boundedElastic());
                                })
                                .onErrorResume(e -> {
                                    (meme).setStatus(MemeStatus.FAILED);
                                    return Mono.fromCallable(() -> memeRepository.save(meme))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .then(Mono.empty());
                                }))
                .subscribe(
                        success -> log.info("Posted meme {}", success.getRedditId()),
                        error -> log.error("Pipeline error", error)
                );
    }
}
