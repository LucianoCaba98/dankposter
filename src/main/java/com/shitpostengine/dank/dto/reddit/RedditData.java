package com.shitpostengine.dank.dto.reddit;

import lombok.Data;

import java.util.List;

@Data
public class RedditData {
    private List<RedditChild> children;
}
