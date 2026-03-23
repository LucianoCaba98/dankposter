package com.dankposter.dto.groq;

import java.util.List;

public record GroqChatResponse(List<GroqChoice> choices) {
}
