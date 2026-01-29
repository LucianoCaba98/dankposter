package com.shitpostengine.dank.externalIntegrations.discord.render;

import com.shitpostengine.dank.externalIntegrations.discord.dto.DiscordMessagePayload;
import com.shitpostengine.dank.model.Meme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemeRenderService {

    private final List<MemeRenderer> renderers;

    public DiscordMessagePayload render(Meme meme) {
        return renderers.stream()
                .filter(r -> r.supports(meme.getSource()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No renderer for source " + meme.getSource()))
                .render(meme);
    }
}
