package com.aiinterview.service.scorer;

import com.aiinterview.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class GeminiLlmClient implements LlmClient {

    private final RestClient restClient;
    private final GeminiProperties properties;

    @Override
    public String name() {
        return "gemini:" + properties.model();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json"));
        try {
            JsonNode response = restClient.post()
                    .uri(properties.baseUrl() + "/v1beta/models/" + properties.model() + ":generateContent")
                    .header("x-goog-api-key", properties.apiKey())
                    .header("content-type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return extractText(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }

    static String extractText(JsonNode response) {
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini 응답이 비어 있습니다");
        }
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            String reason = response.path("candidates").path(0).path("finishReason").asText("");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini가 내용을 반환하지 않았습니다" + (reason.isBlank() ? "" : " (finishReason=" + reason + ")"));
        }
        StringBuilder sb = new StringBuilder();
        parts.forEach(p -> sb.append(p.path("text").asText("")));
        return sb.toString();
    }
}
