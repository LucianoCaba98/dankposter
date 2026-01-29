package com.shitpostengine.dank.externalIntegrations.discord.render;

import com.shitpostengine.dank.externalIntegrations.discord.dto.DiscordMessagePayload;
import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.model.Source;

public interface MemeRenderer {

    boolean supports(Source source);

    DiscordMessagePayload render(Meme meme);
}
