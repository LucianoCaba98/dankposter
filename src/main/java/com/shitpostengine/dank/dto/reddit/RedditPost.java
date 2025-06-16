package com.shitpostengine.dank.dto.reddit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class RedditPost {
    private String title;
    private String url;

    @JsonProperty("ups")
    private int upvotes;

    @JsonProperty("num_comments")
    private int commentCount;

    @JsonProperty("total_awards_received")
    private int awardCount;

    @JsonProperty("over_18")
    private boolean nsfw;

    @JsonProperty("post_hint")
    private String postHint;

    @JsonProperty("link_flair_text")
    private String category;

    @JsonProperty("selftext")
    private String description;

    @JsonProperty("subreddit")
    private String subreddit;
}
