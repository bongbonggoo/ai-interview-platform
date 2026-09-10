package com.aiinterview.service.scorer;

import com.aiinterview.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Claude 생성기와 같은 프롬프트·같은 파서를 쓴다(FeedbackPrompt). 모델만 다르다.
 * 무료 티어가 있어 결제수단 없이 실제 피드백을 확인할 수 있다는 것이 존재 이유다.
 */
@RequiredArgsConstructor
public class GeminiFeedbackGenerator implements FeedbackGenerator {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties properties;

    @Override
    public String name() {
        return "gemini:" + properties.model();
    }

    @Override
    public FeedbackResult generate(FeedbackContext context) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", FeedbackPrompt.system()))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", FeedbackPrompt.user(context))))),
                // JSON으로만 답하게 해서 펜스나 군더더기가 붙을 여지를 줄인다.
                "generationConfig", Map.of("responseMimeType", "application/json"));

        try {
            JsonNode response = restClient.post()
                    .uri(properties.baseUrl() + "/v1beta/models/" + properties.model() + ":generateContent")
                    .header("x-goog-api-key", properties.apiKey())
                    .header("content-type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return FeedbackPrompt.parse(objectMapper, extractText(response));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini 피드백 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }

    static String extractText(JsonNode response) {
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini 응답이 비어 있습니다");
        }
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            // 안전 필터에 걸리면 candidates가 비고 finishReason만 온다 — 조용히 넘기지 않는다.
            String reason = response.path("candidates").path(0).path("finishReason").asText("");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini가 내용을 반환하지 않았습니다" + (reason.isBlank() ? "" : " (finishReason=" + reason + ")"));
        }
        StringBuilder sb = new StringBuilder();
        parts.forEach(p -> sb.append(p.path("text").asText("")));
        return sb.toString();
    }
}
