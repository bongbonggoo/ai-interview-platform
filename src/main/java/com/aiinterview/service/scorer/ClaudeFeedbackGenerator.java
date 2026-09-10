package com.aiinterview.service.scorer;

import com.aiinterview.config.ClaudeProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 사람이 자소서를 들고 와서 "이거 어때요?"라고 물었을 때 하는 피드백을 그대로 재현한다.
 * 즉 회사 인재상을 먼저 놓고, 글에서 그 역량의 근거를 찾아 짚어주고, 빠진 것을 말해준다.
 *
 * 절대 하지 않는 것: 모범답안 제시, 문장 고쳐쓰기, 총점 계산.
 * 앞의 둘은 사용자가 "정답 베끼기"로 빠지게 만들고, 셋째는 원칙 1 위반이다.
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
                "system", systemPrompt(),
                "messages", List.of(Map.of("role", "user", "content", userPrompt(context))));

        try {
            JsonNode response = restClient.post()
                    .uri(properties.baseUrl() + "/v1/messages")
                    .header("x-api-key", properties.apiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return parse(extractText(response));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Claude 피드백 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }

    static String systemPrompt() {
        return """
                당신은 공기업 채용을 준비하는 지원자의 자기소개서나 면접 답변을 읽고 피드백합니다.

                피드백 방식은 이렇습니다.
                먼저 그 기관이 요구하는 역량을 기준으로 삼습니다. 그다음 지원자의 글에서
                그 역량이 드러난 부분을 찾아 원문을 인용하며 짚어주고, 근거가 약하거나
                빠진 부분을 말해줍니다. 각 역량에는 주어진 1/3/5점 행동기준에 비추어 점수를 매깁니다.

                반드시 지킬 것:
                1. 판단은 주어진 역량과 행동기준에만 근거합니다. 당신이 아는 일반적인 자소서 요령으로
                   평가하지 마세요.
                2. strengths와 gaps에는 글에 실제로 있는 내용만 씁니다. 없는 경험을 추측하지 마세요.
                3. quotedEvidence에는 그렇게 판단한 근거가 된 원문을 그대로 발췌합니다.
                   해당하는 내용이 글에 없으면 빈 문자열로 둡니다.
                4. 점수는 1~5 정수입니다. 2점과 4점은 인접한 행동기준 사이에 있을 때 씁니다.

                절대 하지 말 것:
                - 모범답안, 예시 답변, "이렇게 쓰면 좋습니다" 같은 문장 제시
                - 지원자의 문장을 고쳐서 다시 써주기
                - 총점이나 평균 계산 (총점은 시스템이 계산합니다)
                무엇이 부족한지는 말하되, 그 자리를 대신 채워주지는 않습니다.
                지원자가 스스로 자기 경험에서 찾아내야 하기 때문입니다.

                overall에는 어떤 역량이 잘 드러났고 어떤 역량이 약한지를 2~4문장으로 정리합니다.

                설명 없이 아래 JSON만 출력하세요.
                {"overall":"<총평>","verdicts":[{"canonicalId":"<주어진 값 그대로>","score":<1~5>,
                "strengths":"<드러난 부분과 근거>","gaps":"<약하거나 빠진 부분>","quotedEvidence":"<원문 발췌>"}]}

                주어진 모든 역량에 대해 정확히 하나씩 판정하세요.
                """;
    }

    static String userPrompt(FeedbackContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("[지원 기관] ").append(context.companyName()).append('\n');
        if (!context.competencyConfirmed()) {
            sb.append("(이 기관의 인재상 원문을 확보하지 못해 공기업 공통 역량 기준으로만 평가합니다)\n");
        }
        sb.append("[글의 종류] ")
          .append("COVER_LETTER".equals(context.documentType()) ? "자기소개서" : "면접 답변").append('\n');
        if (context.question() != null && !context.question().isBlank()) {
            sb.append("[문항/질문] ").append(context.question()).append('\n');
        }
        sb.append("\n[지원자가 쓴 글]\n").append(context.content()).append("\n\n");
        sb.append("[평가할 역량과 채점 기준]\n");

        for (FeedbackContext.Criterion c : context.criteria()) {
            sb.append("- canonicalId: ").append(c.canonicalId()).append('\n');
            sb.append("  역량: ").append(c.companyLabel());
            if (!c.companyLabel().equals(c.competencyLabel())) {
                sb.append(" (공통 역량 분류: ").append(c.competencyLabel()).append(")");
            }
            sb.append('\n');
            if (c.definition() != null && !c.definition().isBlank()) {
                sb.append("  정의: ").append(c.definition()).append('\n');
            }
            sb.append("  1점: ").append(c.anchor1()).append('\n');
            sb.append("  3점: ").append(c.anchor3()).append('\n');
            sb.append("  5점: ").append(c.anchor5()).append('\n');
        }
        return sb.toString();
    }

    FeedbackResult parse(String text) {
        String json = stripToJsonObject(text);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode verdicts = root.path("verdicts");
            if (!verdicts.isArray() || verdicts.isEmpty()) {
                throw new IllegalArgumentException("verdicts 배열이 없습니다");
            }
            List<CompetencyVerdict> parsed = new ArrayList<>();
            for (JsonNode n : verdicts) {
                parsed.add(new CompetencyVerdict(
                        n.path("canonicalId").asText(null),
                        n.path("score").isInt() ? n.path("score").asInt() : null,
                        n.path("strengths").asText(""),
                        n.path("gaps").asText(""),
                        n.path("quotedEvidence").asText("")));
            }
            return new FeedbackResult(root.path("overall").asText(""), parsed);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Claude 피드백 응답을 해석할 수 없습니다: " + e.getMessage(), e);
        }
    }

    private static String extractText(JsonNode response) {
        if (response == null || !response.path("content").isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Claude 응답 형식이 예상과 다릅니다");
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : response.path("content")) {
            if ("text".equals(block.path("type").asText())) sb.append(block.path("text").asText());
        }
        return sb.toString();
    }

    private static String stripToJsonObject(String text) {
        if (text == null) return "";
        int start = text.indexOf('{'), end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
    }
}
