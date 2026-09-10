package com.aiinterview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gemini는 무료 티어가 있어 결제수단 없이 실제 피드백을 확인할 수 있다.
 * 키는 https://aistudio.google.com/app/apikey 에서 구글 계정만으로 발급된다.
 */
@ConfigurationProperties(prefix = "ai.gemini")
public record GeminiProperties(String apiKey, String model, String baseUrl) {

    public GeminiProperties {
        if (model == null || model.isBlank()) model = "gemini-2.5-flash";
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://generativelanguage.googleapis.com";
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
