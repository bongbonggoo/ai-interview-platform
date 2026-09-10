package com.aiinterview.evaluation;

import com.aiinterview.evaluation.dto.CriterionScore;
import com.aiinterview.evaluation.dto.ScoringRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Claude로 항목별 1~5점과 근거만 받아온다. 총점은 요청하지도 받지도 않는다(원칙 1).
 *
 * 프롬프트에는 이 세션에 고정된 루브릭 버전의 criterion과 1/3/5 앵커만 들어간다.
 * 모델이 자기 기준으로 채점하지 않고 주어진 앵커 문구에만 비추어 판단하도록 지시한다.
 */
@RequiredArgsConstructor
public class ClaudeAnswerScorer implements AnswerScorer {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ClaudeProperties properties;

    @Override
    public String name() {
        return "claude:" + properties.model();
    }

    @Override
    public List<CriterionScore> score(ScoringRequest request) {
        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "max_tokens", properties.maxTokens(),
                "system", systemPrompt(),
                "messages", List.of(Map.of("role", "user", "content", userPrompt(request))));

        String responseText;
        try {
            JsonNode response = restClient.post()
                    .uri(properties.baseUrl() + "/v1/messages")
                    .header("x-api-key", properties.apiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            responseText = extractText(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Claude 채점 호출에 실패했습니다: " + e.getMessage(), e);
        }

        return parseScores(responseText);
    }

    static String systemPrompt() {
        return """
                당신은 공기업 면접 답변을 채점합니다. 반드시 아래 규칙을 지키세요.

                1. 주어진 평가 기준과 1/3/5점 행동기준(앵커) 문구에만 비추어 판단합니다.
                   당신이 알고 있는 일반적인 면접 상식이나 개인적 기준으로 채점하지 마세요.
                2. 각 기준마다 1~5점 정수 하나를 매깁니다. 2점과 4점은 인접한 앵커 사이에 있을 때 씁니다.
                3. 근거(evidence)에는 답변에서 실제로 그렇게 판단한 부분을 인용하거나 지목해서
                   한두 문장으로 적습니다. 답변에 없는 내용을 추측해서 쓰지 마세요.
                4. 총점이나 평균을 계산하지 마세요. 항목별 점수만 냅니다.
                5. 설명 없이 아래 JSON만 출력하세요.

                {"scores":[{"criterionId":"<주어진 criterionId 그대로>","score":<1~5>,"evidence":"<근거>"}]}

                주어진 모든 기준에 대해 정확히 하나씩, 빠짐없이 채점하세요.
                """;
    }

    static String userPrompt(ScoringRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[회사] ").append(request.companyName()).append('\n');
        sb.append("[직무] ").append(request.jobPositionName()).append('\n');
        sb.append("[질문] ").append(request.questionText()).append('\n');
        sb.append("[지원자 답변]\n").append(request.answerText()).append("\n\n");
        sb.append("[평가 기준]\n");

        for (ScoringRequest.Criterion criterion : request.criteria()) {
            sb.append("- criterionId: ").append(criterion.criterionId()).append('\n');
            sb.append("  역량: ").append(criterion.label())
              .append(" (").append(criterion.canonicalId()).append(", 가중치 ")
              .append(criterion.weightPct()).append("%)\n");
            if (criterion.definition() != null && !criterion.definition().isBlank()) {
                sb.append("  정의: ").append(criterion.definition()).append('\n');
            }
            sb.append("  1점: ").append(criterion.anchorLevel1()).append('\n');
            sb.append("  3점: ").append(criterion.anchorLevel3()).append('\n');
            sb.append("  5점: ").append(criterion.anchorLevel5()).append('\n');
        }
        return sb.toString();
    }

    private static String extractText(JsonNode response) {
        if (response == null || !response.path("content").isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Claude 응답 형식이 예상과 다릅니다");
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : response.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                sb.append(block.path("text").asText());
            }
        }
        return sb.toString();
    }

    /** 모델이 ```json 펜스를 붙이거나 앞뒤에 말을 덧붙여도 JSON 본체만 뽑아낸다. */
    List<CriterionScore> parseScores(String text) {
        String json = stripToJsonObject(text);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode scores = root.path("scores");
            if (!scores.isArray() || scores.isEmpty()) {
                throw new IllegalArgumentException("scores 배열이 없습니다");
            }

            List<CriterionScore> parsed = new ArrayList<>();
            for (JsonNode node : scores) {
                parsed.add(new CriterionScore(
                        UUID.fromString(node.path("criterionId").asText()),
                        node.path("score").isInt() ? node.path("score").asInt() : null,
                        node.path("evidence").asText(null)));
            }
            return parsed;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Claude 채점 응답을 해석할 수 없습니다: " + e.getMessage(), e);
        }
    }

    private static String stripToJsonObject(String text) {
        if (text == null) return "";
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
    }
}
