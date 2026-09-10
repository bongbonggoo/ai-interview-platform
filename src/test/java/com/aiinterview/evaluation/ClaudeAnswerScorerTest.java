package com.aiinterview.evaluation;

import com.aiinterview.evaluation.dto.CriterionScore;
import com.aiinterview.evaluation.dto.ScoringRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 네트워크 없이 프롬프트 구성과 응답 파싱만 검증한다. */
class ClaudeAnswerScorerTest {

    private final ClaudeAnswerScorer scorer = new ClaudeAnswerScorer(
            null, new ObjectMapper(),
            new ClaudeProperties("test-key", null, null, null));

    private static final UUID CRITERION_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    @DisplayName("프롬프트에 criterionId와 1/3/5 앵커가 그대로 들어간다")
    void promptCarriesAnchors() {
        String prompt = ClaudeAnswerScorer.userPrompt(new ScoringRequest(
                "한국철도공사", "사무영업", "고객 항의 대응은?", "대안을 제시하겠습니다",
                List.of(new ScoringRequest.Criterion(CRITERION_ID, "CUSTOMER_ORIENTATION", "고객지향",
                        "고객 관점에서 판단", 60, "요구를 무시함", "규정 내 대응", "대안 제시"))));

        assertThat(prompt).contains(CRITERION_ID.toString())
                .contains("고객지향").contains("60%")
                .contains("1점: 요구를 무시함").contains("3점: 규정 내 대응").contains("5점: 대안 제시");
    }

    @Test
    @DisplayName("시스템 프롬프트는 총점 계산을 금지한다")
    void systemPromptForbidsTotals() {
        assertThat(ClaudeAnswerScorer.systemPrompt()).contains("총점이나 평균을 계산하지 마세요");
    }

    @Test
    @DisplayName("```json 펜스나 앞뒤 군더더기가 있어도 JSON을 뽑아낸다")
    void parsesFencedJson() {
        List<CriterionScore> scores = scorer.parseScores("""
                채점 결과입니다.
                ```json
                {"scores":[{"criterionId":"%s","score":4,"evidence":"대안을 제시함"}]}
                ```
                """.formatted(CRITERION_ID));

        assertThat(scores).singleElement().satisfies(s -> {
            assertThat(s.criterionId()).isEqualTo(CRITERION_ID);
            assertThat(s.score()).isEqualTo(4);
            assertThat(s.evidence()).isEqualTo("대안을 제시함");
        });
    }

    @Test
    @DisplayName("JSON이 아니면 502로 실패한다 — 못 읽은 응답을 그냥 넘기지 않는다")
    void rejectsNonJson() {
        assertThatThrownBy(() -> scorer.parseScores("죄송하지만 채점할 수 없습니다"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
