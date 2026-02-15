package com.dankposter.externalIntegrations.discord.render;

import com.dankposter.externalIntegrations.discord.dto.DiscordMessagePayload;
import com.dankposter.model.Meme;
import com.dankposter.model.Source;

public interface MemeRenderer {

    boolean supports(Source source);

    DiscordMessagePayload render(Meme meme);
}
