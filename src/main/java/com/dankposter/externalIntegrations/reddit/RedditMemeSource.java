package com.dankposter.externalIntegrations.reddit;

import com.dankposter.config.RedditProperties;
import com.dankposter.dto.reddit.RedditChild;
import com.dankposter.dto.reddit.RedditResponse;
import com.dankposter.model.Meme;
import com.dankposter.model.MemeSource;
import com.dankposter.model.MemeStatus;
import com.dankposter.model.Source;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;

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

    private static final List<String> SORT_MODES = List.of("hot", "new");

    public Flux<Meme> fetch() {
        log.info("Fetching memes from reddit ({} subreddits × {} modes)", 
                redditProperties.getSubreddits().size(), SORT_MODES.size());
        return Flux.fromIterable(redditProperties.getSubreddits())
                .concatMap(source -> Flux.fromIterable(SORT_MODES)
                        .concatMap(mode -> {
                            String url = String.format("https://www.reddit.com/r/%s/%s.json?limit=%d",
                                    source.getName(), mode, source.getLimit());
                            return Mono.delay(Duration.ofMillis(1500))
                                    .then(webClient
                                            .get()
                                            .uri(URI.create(url))
                                            .header("User-Agent", "DankPoster/1.0")
                                            .retrieve()
                                            .bodyToMono(RedditResponse.class)
                                            .doOnNext(r -> log.info("Fetched {} posts from r/{}/{}", 
                                                    r.getData().getChildren().size(), source.getName(), mode))
                                            .onErrorResume(e -> {
                                                log.warn("Error fetching r/{}/{}: {}", source.getName(), mode, e.getMessage());
                                                return Mono.empty();
                                            }));
                        }))
                .flatMap(response -> Flux.fromIterable(response.getData().getChildren()))
                .map(RedditChild::getData)
                .filter(post -> post.getUrl() != null && !post.getUrl().isBlank())
                .filter(post -> {
                    String hint = post.getPostHint();
                    if ("image".equals(hint)) return true;
                    if (hint == null) {
                        // No hint — accept if URL looks like direct media
                        String lower = post.getUrl().toLowerCase();
                        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                                || lower.endsWith(".gif") || lower.endsWith(".gifv");
                    }
                    return false;
                })
                .map(meme -> Meme.builder()
                        .externalId(meme.getId())
                        .description(meme.getDescription())
                        .title(meme.getTitle() != null && meme.getTitle().length() > 500
                                ? meme.getTitle().substring(0, 497) + "..."
                                : meme.getTitle())
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
