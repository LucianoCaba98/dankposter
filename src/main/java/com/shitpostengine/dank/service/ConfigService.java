package com.shitpostengine.dank.service;

import com.shitpostengine.dank.config.DiscordConfig;
import com.shitpostengine.dank.config.KafkaProperties;
import com.shitpostengine.dank.config.RedditProperties;
import com.shitpostengine.dank.config.SchedulingProperties;
import com.shitpostengine.dank.config.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigService {

    private final SchedulingProperties schedulingProperties;
    private final SqsProperties sqsProperties;
    private final KafkaProperties kafkaProperties;
    private final DiscordConfig discordConfig;
    private final RedditProperties redditProperties;

    public Map<String, Object> getAllConfig() {
        Map<String, Object> config = new LinkedHashMap<>();

        config.put("scheduling", Map.of(
                "fetchIntervalMs", schedulingProperties.getFetchIntervalMs(),
                "postIntervalMs", schedulingProperties.getPostIntervalMs()
        ));

        config.put("sqs", Map.of(
                "enabled", sqsProperties.isEnabled(),
                "queueUrl", sqsProperties.getQueueUrl() != null ? sqsProperties.getQueueUrl() : "",
                "dlqUrl", sqsProperties.getDlqUrl() != null ? sqsProperties.getDlqUrl() : "",
                "region", sqsProperties.getRegion() != null ? sqsProperties.getRegion() : "",
                "pollInterval", sqsProperties.getPollInterval() != null ? sqsProperties.getPollInterval().getSeconds() : 0L
        ));

        config.put("kafka", Map.of(
                "enabled", kafkaProperties.isEnabled(),
                "bootstrapServers", kafkaProperties.getBootstrapServers() != null ? kafkaProperties.getBootstrapServers() : "",
                "topic", kafkaProperties.getTopic() != null ? kafkaProperties.getTopic() : "",
                "consumerGroup", kafkaProperties.getConsumerGroup() != null ? kafkaProperties.getConsumerGroup() : ""
        ));

        config.put("discord", Map.of(
                "botToken", "***",
                "channelId", discordConfig.getChannelId() != null ? discordConfig.getChannelId() : ""
        ));

        List<Map<String, Object>> subreddits = new ArrayList<>();
        if (redditProperties.getSubreddits() != null) {
            for (RedditProperties.Subreddit sub : redditProperties.getSubreddits()) {
                subreddits.add(Map.of(
                        "name", sub.getName() != null ? sub.getName() : "",
                        "limit", sub.getLimit()
                ));
            }
        }
        config.put("redditSubreddits", subreddits);

        return config;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> updateCategory(String category, Map<String, Object> values) {
        switch (category) {
            case "scheduling" -> updateScheduling(values);
            case "sqs" -> updateSqs(values);
            case "kafka" -> updateKafka(values);
            case "discord" -> updateDiscord(values);
            case "redditSubreddits" -> updateRedditSubreddits(values);
            default -> throw new IllegalArgumentException("Unknown category: " + category);
        }

        Map<String, Object> allConfig = getAllConfig();
        Object updated = allConfig.get(category);
        if (updated instanceof Map) {
            return (Map<String, Object>) updated;
        }
        // For redditSubreddits which is a List, wrap it
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(category, updated);
        return result;
    }

    private void updateScheduling(Map<String, Object> values) {
        if (values.containsKey("fetchIntervalMs")) {
            long val = toLong(values.get("fetchIntervalMs"));
            if (val <= 0) {
                throw new IllegalArgumentException("fetchIntervalMs must be positive");
            }
            schedulingProperties.setFetchIntervalMs(val);
        }
        if (values.containsKey("postIntervalMs")) {
            long val = toLong(values.get("postIntervalMs"));
            if (val <= 0) {
                throw new IllegalArgumentException("postIntervalMs must be positive");
            }
            schedulingProperties.setPostIntervalMs(val);
        }
    }

    private void updateSqs(Map<String, Object> values) {
        boolean enabled = sqsProperties.isEnabled();
        if (values.containsKey("enabled")) {
            enabled = toBoolean(values.get("enabled"));
        }

        if (enabled) {
            String queueUrl = values.containsKey("queueUrl")
                    ? String.valueOf(values.get("queueUrl")).trim()
                    : (sqsProperties.getQueueUrl() != null ? sqsProperties.getQueueUrl() : "");
            if (queueUrl.isEmpty()) {
                throw new IllegalArgumentException("queueUrl must not be empty when SQS is enabled");
            }

            String dlqUrl = values.containsKey("dlqUrl")
                    ? String.valueOf(values.get("dlqUrl")).trim()
                    : (sqsProperties.getDlqUrl() != null ? sqsProperties.getDlqUrl() : "");
            if (dlqUrl.isEmpty()) {
                throw new IllegalArgumentException("dlqUrl must not be empty when SQS is enabled");
            }
        }

        if (values.containsKey("pollInterval")) {
            long val = toLong(values.get("pollInterval"));
            if (val <= 0) {
                throw new IllegalArgumentException("pollInterval must be positive");
            }
            sqsProperties.setPollInterval(Duration.ofSeconds(val));
        }

        if (values.containsKey("enabled")) {
            sqsProperties.setEnabled(enabled);
        }
        if (values.containsKey("queueUrl")) {
            sqsProperties.setQueueUrl(String.valueOf(values.get("queueUrl")).trim());
        }
        if (values.containsKey("dlqUrl")) {
            sqsProperties.setDlqUrl(String.valueOf(values.get("dlqUrl")).trim());
        }
        if (values.containsKey("region")) {
            sqsProperties.setRegion(String.valueOf(values.get("region")).trim());
        }
    }

    private void updateKafka(Map<String, Object> values) {
        boolean enabled = kafkaProperties.isEnabled();
        if (values.containsKey("enabled")) {
            enabled = toBoolean(values.get("enabled"));
        }

        if (enabled) {
            String bootstrapServers = values.containsKey("bootstrapServers")
                    ? String.valueOf(values.get("bootstrapServers")).trim()
                    : (kafkaProperties.getBootstrapServers() != null ? kafkaProperties.getBootstrapServers() : "");
            if (bootstrapServers.isEmpty()) {
                throw new IllegalArgumentException("bootstrapServers must not be empty when Kafka is enabled");
            }
        }

        if (values.containsKey("enabled")) {
            kafkaProperties.setEnabled(enabled);
        }
        if (values.containsKey("bootstrapServers")) {
            kafkaProperties.setBootstrapServers(String.valueOf(values.get("bootstrapServers")).trim());
        }
        if (values.containsKey("topic")) {
            kafkaProperties.setTopic(String.valueOf(values.get("topic")).trim());
        }
        if (values.containsKey("consumerGroup")) {
            kafkaProperties.setConsumerGroup(String.valueOf(values.get("consumerGroup")).trim());
        }
    }

    private void updateDiscord(Map<String, Object> values) {
        if (values.containsKey("channelId")) {
            String channelId = String.valueOf(values.get("channelId")).trim();
            if (channelId.isEmpty()) {
                throw new IllegalArgumentException("channelId must not be empty");
            }
            discordConfig.setChannelId(channelId);
        }
        if (values.containsKey("botToken")) {
            String botToken = String.valueOf(values.get("botToken")).trim();
            // Don't update if masked value is sent back
            if (!"***".equals(botToken)) {
                discordConfig.setBotToken(botToken);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateRedditSubreddits(Map<String, Object> values) {
        Object subredditsObj = values.get("subreddits");
        if (subredditsObj == null) {
            subredditsObj = values.get("redditSubreddits");
        }
        if (subredditsObj instanceof List<?> list) {
            List<RedditProperties.Subreddit> subreddits = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String name = map.get("name") != null ? String.valueOf(map.get("name")).trim() : "";
                    if (name.isEmpty()) {
                        throw new IllegalArgumentException("Subreddit name must not be empty");
                    }
                    int limit = toInt(map.get("limit"));
                    if (limit < 1) {
                        throw new IllegalArgumentException("Subreddit limit must be at least 1");
                    }
                    RedditProperties.Subreddit sub = new RedditProperties.Subreddit();
                    sub.setName(name);
                    sub.setLimit(limit);
                    subreddits.add(sub);
                }
            }
            redditProperties.setSubreddits(subreddits);
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number num) {
            return num.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private int toInt(Object value) {
        if (value instanceof Number num) {
            return num.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
