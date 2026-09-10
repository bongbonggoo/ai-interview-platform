package com.aiinterview.service.scorer;

import com.aiinterview.config.ClaudeProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 사람이 자소서를 들고 와서 "이거 어때요?"라고 물었을 때 하는 피드백을 그대로 재현한다.
 * 즉 회사 인재상을 먼저 놓고, 글에서 그 역량의 근거를 찾아 짚어주고, 빠진 것을 말해준다.
 *
 * 절대 하지 않는 것: 모범답안 제시, 문장 고쳐쓰기, 총점 계산.
 * 앞의 둘은 사용자가 "정답 베끼기"로 빠지게 만들고, 셋째는 원칙 1 위반이다.
 * 프롬프트와 파싱은 FeedbackPrompt에 모아둬서 Gemini 생성기와 성격이 갈리지 않게 한다.
 */
@RequiredArgsConstructor
public class ClaudeFeedbackGenerator implements FeedbackGenerator {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ClaudeProperties properties;

    @Override
    public String name() {
        return "claude:" + properties.model();
    }

    @Override
    public FeedbackResult generate(FeedbackContext context) {
        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "max_tokens", properties.maxTokens(),
                "system", FeedbackPrompt.system(),
                "messages", List.of(Map.of("role", "user", "content", FeedbackPrompt.user(context))));

        try {
            JsonNode response = restClient.post()
                    .uri(properties.baseUrl() + "/v1/messages")
                    .header("x-api-key", properties.apiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return FeedbackPrompt.parse(objectMapper, extractText(response));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Claude 피드백 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }

    static String extractText(JsonNode response) {
        if (response == null || !response.path("content").isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Claude 응답 형식이 예상과 다릅니다");
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : response.path("content")) {
            if ("text".equals(block.path("type").asText())) sb.append(block.path("text").asText());
        }
        return sb.toString();
    }
}
