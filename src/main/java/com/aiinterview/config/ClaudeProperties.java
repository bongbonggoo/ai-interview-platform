package com.aiinterview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 키가 비어 있으면 Claude 대신 스텁 채점기가 뜬다(로컬에서 키 없이 전체 흐름을 돌려볼 수 있게). */
@ConfigurationProperties(prefix = "ai.claude")
public record ClaudeProperties(
        String apiKey,
        String model,
        String baseUrl,
        Integer maxTokens
) {
    public ClaudeProperties {
        if (model == null || model.isBlank()) model = "claude-sonnet-5";
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.anthropic.com";
        if (maxTokens == null) maxTokens = 2048;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
