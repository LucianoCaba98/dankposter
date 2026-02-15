package com.dankposter.externalIntegrations.discord.render;

import com.dankposter.externalIntegrations.discord.dto.DiscordEmbed;
import com.dankposter.externalIntegrations.discord.dto.DiscordMessagePayload;
import com.dankposter.model.Meme;
import com.dankposter.model.Source;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GiphyMemeRenderer implements MemeRenderer {

    @Override
    public boolean supports(Source source) {
        return source == Source.GIPHY;
    }

    @Override
    public DiscordMessagePayload render(Meme meme) {
        DiscordEmbed embed = new DiscordEmbed();
        embed.setTitle(meme.getTitle());
        embed.setColor(0x9B59B6); // violeta sexy

        DiscordEmbed.EmbedImage image = new DiscordEmbed.EmbedImage();
        image.setUrl(meme.getImageUrl()); // GIF REAL (downsized/original)
        embed.setImage(image);

        DiscordEmbed.EmbedFooter footer = new DiscordEmbed.EmbedFooter();
        footer.setText("via GIPHY");
        embed.setFooter(footer);

        DiscordMessagePayload payload = new DiscordMessagePayload();
        payload.setEmbeds(List.of(embed));
        payload.setContent(null); // MUY IMPORTANTE: NO mandar el link en content

        return payload;
    }
}
