package com.shitpostengine.dank.externalIntegrations.reddit;

import com.shitpostengine.dank.config.RedditProperties;
import com.shitpostengine.dank.dto.reddit.RedditChild;
import com.shitpostengine.dank.dto.reddit.RedditResponse;
import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.model.MemeSource;
import com.shitpostengine.dank.model.MemeStatus;
import com.shitpostengine.dank.model.Source;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "meme.sources",
        name = "reddit",
        havingValue = "true"
)
public class RedditMemeSource implements MemeSource {

    private final WebClient webClient;
    private final RedditProperties redditProperties;

    public Flux<Meme> fetch() {
        log.info("fetching memes from reddit #hard");
        return Flux.fromIterable(redditProperties.getSubreddits())

                .flatMap(source -> {
                    String url = String.format("https://www.reddit.com/r/%s/hot.json?limit=%d", source.getName(), source.getLimit());
                    return webClient
                            .get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(RedditResponse.class)
                            .onErrorResume(e -> {
                                log.warn("Error fetching r/{}", source.getName(), e);
                                return Mono.empty();
                            });
                }, 2)
                .flatMap(response -> Flux.fromIterable(response.getData().getChildren()))
                .map(RedditChild::getData)
                .map(meme -> Meme.builder()
                        .externalId(meme.getId())
                        .description(meme.getDescription())
                        .title(meme.getTitle())
                        .imageUrl(meme.getUrl())
                        .status(MemeStatus.FETCHED)
                        .source(Source.REDDIT)
                        .build());
    }

    @Override
    public String sourceName() {
        return "REDDIT";
    }
}
