package com.dankposter.externalIntegrations.discord.render;

import com.dankposter.externalIntegrations.discord.dto.DiscordMessagePayload;
import com.dankposter.model.Meme;
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
                .orElseThrow(() -> new IllegalStateException("No renderer for source " + meme.getSource()))
                .render(meme);
    }
}
