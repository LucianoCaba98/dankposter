package com.shitpostengine.dank.scheduler;

import com.shitpostengine.dank.service.DiscordPosterService;
import com.shitpostengine.dank.service.RedditFetcherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MemeScheduler {

    private final RedditFetcherService redditFetcherService;
    private final DiscordPosterService discordPosterService;

    public MemeScheduler(RedditFetcherService redditFetcherService, DiscordPosterService discordPosterService) {
        this.redditFetcherService = redditFetcherService;
        this.discordPosterService = discordPosterService;
    }

    @Scheduled(fixedRateString = "${scheduling.fetch-interval-ms}")
    public void memeFetcher() {
        redditFetcherService.fetchMemesFromSubreddit();
    }

    @Scheduled(fixedRateString = "${scheduling.post-interval-ms}")
    public void memePoster() {
        discordPosterService.postNextUnpostedMeme();
    }
}
