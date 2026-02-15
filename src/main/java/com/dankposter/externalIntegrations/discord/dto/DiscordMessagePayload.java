package com.dankposter.externalIntegrations.discord.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordMessagePayload {

    private String content;
    private List<DiscordEmbed> embeds;

}
