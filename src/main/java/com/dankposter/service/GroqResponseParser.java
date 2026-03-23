package com.dankposter.service;

import com.dankposter.dto.groq.GroqDescriptionEntry;
import com.dankposter.dto.groq.GroqScoreEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Stateless utility for parsing Groq chat completions responses.
 * Extracts content, strips markdown fences, and deserializes score/description entries.
 */
@Slf4j
public final class GroqResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern MARKDOWN_FENCE = Pattern.compile(
            "^\\s*```(?:json)?\\s*\\n?(.*?)\\n?\\s*```\\s*$", Pattern.DOTALL);

    private GroqResponseParser() {}

    /**
     * Extracts choices[0].message.content from a Groq chat completions JSON response body.
     *
     * @param responseBody the raw JSON response from Groq
     * @return the content string, or null if parsing fails or structure is invalid
     */
    public static String extractContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode content = choices.get(0).path("message").path("content");
            return content.isMissingNode() || content.isNull() ? null : content.asText();
        } catch (Exception e) {
            log.warn("Failed to extract content from Groq response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Removes markdown code fences (```json ... ``` or ``` ... ```) if present.
     * Idempotent: applying twice yields the same result as applying once.
     *
     * @param content the string potentially wrapped in markdown fences
     * @return the inner content without fences, or the original string if no fences found
     */
    public static String stripMarkdownFences(String content) {
        if (content == null) {
            return null;
        }
        var matcher = MARKDOWN_FENCE.matcher(content);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return content;
    }

    /**
     * Parses a JSON array of objects with "title" and "score" fields.
     * Scores are clamped to [0.0, 1.0] via {@link #clampScore(double)}.
     *
     * @param json the JSON array string
     * @return list of score entries, or empty list on parse failure
     */
    public static List<GroqScoreEntry> parseScores(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            JsonNode array = MAPPER.readTree(json);
            if (!array.isArray()) {
                log.warn("Expected JSON array for scores, got: {}", array.getNodeType());
                return Collections.emptyList();
            }
            List<GroqScoreEntry> entries = new ArrayList<>();
            for (JsonNode node : array) {
                String title = node.path("title").asText(null);
                double score = node.path("score").asDouble(0.0);
                if (title != null) {
                    entries.add(new GroqScoreEntry(title, clampScore(score)));
                }
            }
            return entries;
        } catch (Exception e) {
            log.warn("Failed to parse score entries: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Parses a JSON array of objects with "title" and "description" fields.
     *
     * @param json the JSON array string
     * @return list of description entries, or empty list on parse failure
     */
    public static List<GroqDescriptionEntry> parseDescriptions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            JsonNode array = MAPPER.readTree(json);
            if (!array.isArray()) {
                log.warn("Expected JSON array for descriptions, got: {}", array.getNodeType());
                return Collections.emptyList();
            }
            List<GroqDescriptionEntry> entries = new ArrayList<>();
            for (JsonNode node : array) {
                String title = node.path("title").asText(null);
                String description = node.path("description").asText(null);
                if (title != null) {
                    entries.add(new GroqDescriptionEntry(title, description));
                }
            }
            return entries;
        } catch (Exception e) {
            log.warn("Failed to parse description entries: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Clamps a score to the [0.0, 1.0] range.
     *
     * @param score the raw score value
     * @return the clamped score
     */
    public static double clampScore(double score) {
        return Math.max(0.0, Math.min(1.0, score));
    }
}
