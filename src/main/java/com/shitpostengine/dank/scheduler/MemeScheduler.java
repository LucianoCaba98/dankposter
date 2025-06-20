package com.shitpostengine.dank.scheduler;

import com.shitpostengine.dank.service.RedditMemeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MemeScheduler {

    private final RedditMemeService redditMemeService;

    public MemeScheduler(RedditMemeService redditMemeService) {
        this.redditMemeService = redditMemeService;
    }

    @Scheduled(fixedRateString = "${scheduling.fetch-interval-ms}")
    public void memeScheduler() {
        redditMemeService.fetchMemesFromSubreddit();
    }
}
