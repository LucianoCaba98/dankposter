package com.shitpostengine.dank.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
@Slf4j
public class MemeFetchPipeline {

    private final RedditFetcherService redditFetcherService;

    public MemeFetchPipeline(RedditFetcherService redditFetcherService) {
        this.redditFetcherService = redditFetcherService;
    }

    @PostConstruct
    public void start() {
        Flux.interval(Duration.ofSeconds(30))
                .flatMap(tick -> redditFetcherService.fetch())
                .subscribe(
                meme -> log.info("Fetched meme {}", meme.getRedditId()),
                error -> log.error("Pipeline crashed", error)
        );;
    }
}
