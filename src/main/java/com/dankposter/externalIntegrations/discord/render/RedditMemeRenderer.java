package com.dankposter.externalIntegrations.discord.render;

import com.dankposter.externalIntegrations.discord.dto.DiscordEmbed;
import com.dankposter.externalIntegrations.discord.dto.DiscordMessagePayload;
import com.dankposter.model.Meme;
import com.dankposter.model.Source;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedditMemeRenderer implements MemeRenderer {

    @Override
    public boolean supports(Source source) {
        return source == Source.REDDIT;
    }

    @Override
    public DiscordMessagePayload render(Meme meme) {
        DiscordEmbed embed = new DiscordEmbed();
        embed.setTitle(meme.getTitle());
        embed.setColor(0xFF4500);

        DiscordEmbed.EmbedImage image = new DiscordEmbed.EmbedImage();
        image.setUrl(meme.getImageUrl());
        embed.setImage(image);

        DiscordMessagePayload payload = new DiscordMessagePayload();
        payload.setEmbeds(List.of(embed));
        payload.setContent(null);

        return payload;
    }
}
