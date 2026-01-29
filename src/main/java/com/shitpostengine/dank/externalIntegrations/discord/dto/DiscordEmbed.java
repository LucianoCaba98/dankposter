package com.shitpostengine.dank.externalIntegrations.discord.dto;

import lombok.Data;

@Data
public class DiscordEmbed {

    private String title;
    private String description;
    private String url;
    private Integer color;

    private EmbedImage image;
    private EmbedVideo video;
    private EmbedFooter footer;

    @Data
    public static class EmbedImage {
        private String url;
    }

    @Data
    public static class EmbedVideo {
        private String url;
    }

    @Data
    public static class EmbedFooter {
        private String text;
    }
}
