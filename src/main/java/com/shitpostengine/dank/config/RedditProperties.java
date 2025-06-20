package com.shitpostengine.dank.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "reddit")
public class RedditProperties {
    private List<Subreddit> subreddits;

    @Data
    public static class Subreddit {
        private String name;
        private int limit;
    }
}