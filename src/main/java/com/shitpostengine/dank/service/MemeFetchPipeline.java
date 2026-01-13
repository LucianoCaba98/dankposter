package com.shitpostengine.dank.service;

import com.shitpostengine.dank.model.Meme;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
@Slf4j
public class MemeFetchPipeline {

    private final RedditFetcherService redditFetcherService;
    private final DiscordPosterService discordPosterService;

    public MemeFetchPipeline(RedditFetcherService redditFetcherService, DiscordPosterService discordPosterService) {
        this.redditFetcherService = redditFetcherService;
        this.discordPosterService = discordPosterService;
    }

    @PostConstruct
    public void start() {
        Flux.interval(Duration.ofSeconds(30))
                .flatMap(tick -> redditFetcherService.fetch())
                .delayElements(Duration.ofSeconds(2))
                .concatMap(discordPosterService::post)
                .subscribe(
                        success -> log.info("Posted meme {}", success.getRedditId()),
                        error -> log.error("Pipeline error", error)
                );
    }
}
