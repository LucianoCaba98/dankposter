package com.shitpostengine.dank.dto.reddit;

import com.shitpostengine.dank.model.Meme;
import lombok.Data;
import reactor.core.publisher.Flux;

@Data
public class RedditResponse {
    private RedditData data;
}
