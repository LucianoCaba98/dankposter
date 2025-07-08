package com.shitpostengine.dank.scheduler;

import com.shitpostengine.dank.service.DiscordPosterService;
import com.shitpostengine.dank.service.RedditMemeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MemeScheduler {

    private final RedditMemeService redditMemeService;
    private final DiscordPosterService discordPosterService;

    public MemeScheduler(RedditMemeService redditMemeService, DiscordPosterService discordPosterService) {
        this.redditMemeService = redditMemeService;
        this.discordPosterService = discordPosterService;
    }

    @Scheduled(fixedRateString = "${scheduling.fetch-interval-ms}")
    public void memeFetcher() {
        redditMemeService.fetchMemesFromSubreddit();
    }

    @Scheduled(fixedRateString = "${scheduling.post-interval-ms}")
    public void memePoster() {
        discordPosterService.postNextUnpostedMeme();
    }
}
