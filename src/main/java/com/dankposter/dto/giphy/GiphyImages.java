package com.dankposter.dto.giphy;

import lombok.Data;

@Data
public class GiphyImages {

    private GiphyImage original;        // gif pesado
    private GiphyImage downsized;       // 👈 GIF ANIMADO IDEAL
    private GiphyImage fixed_height;    // alternativa
    private GiphyImage original_still;  // ❌ imagen estática
    private GiphyImage preview;         // ❌ video
}
