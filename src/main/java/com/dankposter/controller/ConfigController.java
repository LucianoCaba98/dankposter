package com.dankposter.controller;

import com.dankposter.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public Map<String, Object> getAllConfig() {
        return configService.getAllConfig();
    }

    @PutMapping("/{category}")
    public ResponseEntity<Map<String, Object>> updateConfig(
            @PathVariable String category,
            @RequestBody Map<String, Object> values) {
        try {
            Map<String, Object> updated = configService.updateCategory(category, values);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("Unknown category:")) {
                return ResponseEntity.status(404).body(Map.of("error", message));
            }
            return ResponseEntity.badRequest().body(
                    Map.of("error", message, "field", extractFieldFromMessage(message))
            );
        }
    }

    private String extractFieldFromMessage(String message) {
        if (message == null) return "unknown";
        int idx = message.indexOf(" must");
        if (idx > 0) return message.substring(0, idx);
        return "unknown";
    }
}
