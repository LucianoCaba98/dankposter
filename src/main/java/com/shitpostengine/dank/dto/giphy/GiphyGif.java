package com.shitpostengine.dank.dto.giphy;

import lombok.Data;

@Data
public class GiphyGif {
    private String id;
    private String title;
    private String url;
    private GiphyImages images;
}
