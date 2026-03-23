package com.dankposter.dto.d3vd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class D3vdPost {
    private String postLink;
    private String subreddit;
    private String title;
    private String url;
    private boolean nsfw;
    private boolean spoiler;
    private String author;
    private int ups;
}
