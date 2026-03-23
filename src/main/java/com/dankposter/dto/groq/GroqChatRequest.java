package com.dankposter.dto.groq;

import java.util.List;

public record GroqChatRequest(String model, List<GroqMessage> messages, int max_tokens) {
}
