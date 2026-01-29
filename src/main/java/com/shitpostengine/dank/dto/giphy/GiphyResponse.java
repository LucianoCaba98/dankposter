package com.shitpostengine.dank.dto.giphy;

import lombok.Data;
import java.util.List;

@Data
public class GiphyResponse {
    private List<GiphyGif> data;
}