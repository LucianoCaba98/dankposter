package com.shitpostengine.dank.scheduler;

import com.shitpostengine.dank.config.KafkaProperties;
import com.shitpostengine.dank.service.DiscordPosterService;
import com.shitpostengine.dank.service.RedditFetcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MemeScheduler {

    private final RedditFetcherService redditFetcherService;
    private final DiscordPosterService discordPosterService;
    private final KafkaProperties kafkaProperties;

    public MemeScheduler(RedditFetcherService redditFetcherService, DiscordPosterService discordPosterService, KafkaProperties kafkaProperties) {
        this.redditFetcherService = redditFetcherService;
        this.discordPosterService = discordPosterService;
        this.kafkaProperties = kafkaProperties;
    }

    @Scheduled(fixedRateString = "${scheduling.fetch-interval-ms}")
    public void memeFetcher() {
        redditFetcherService.fetchMemesFromSubreddit();
    }

    @Scheduled(fixedRateString = "${scheduling.post-interval-ms}")
    public void memePoster() {
        if (kafkaProperties.isEnabled()) {
            log.debug("Kafka is enabled — skipping scheduled meme posting (Kafka consumer handles delivery)");
            return;
        }
        discordPosterService.postNextUnpostedMeme();
    }
}
