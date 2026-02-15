package com.dankposter.dto.giphy;

import lombok.Data;

@Data
public class GiphyImages {

    private GiphyImage original;
    private GiphyImage downsized;
    private GiphyImage fixed_height;
    private GiphyImage original_still;
    private GiphyImage preview;
}
