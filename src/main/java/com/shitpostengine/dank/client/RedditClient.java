package com.shitpostengine.dank.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class RedditClient {

    private final WebClient webClient;

    public RedditClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> getHotMemesJson(String subreddit, int limit) {



        return webClient.get()
                .uri("https://www.reddit.com/r/{subreddit}/hot.json?limit={limit}", subreddit, limit)
                .header("User-Agent", "dank-poster-bot/0.1")
                .retrieve()
                .bodyToMono(String.class);
    }
}