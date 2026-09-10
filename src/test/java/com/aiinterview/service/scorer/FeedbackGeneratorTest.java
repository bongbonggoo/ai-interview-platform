package com.aiinterview.service.scorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 네트워크 없이 프롬프트 구성과 응답 파싱만 본다. */
class FeedbackGeneratorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static FeedbackContext context() {
        return new FeedbackContext("한국철도공사", true, "COVER_LETTER", "지원동기", "내용".repeat(30),
                List.of(new FeedbackContext.Criterion("COMMUNICATION", "사람지향 소통인", "의사소통능력",
                        "상대와 상황에 맞게 전달", "근거 불분명", "요점 정리됨", "이견 조율 확인")));
    }

    @Test
    @DisplayName("프롬프트가 모범답안을 금지하고, 회사 표현과 1/3/5 기준을 함께 싣는다")
    void promptForbidsModelAnswers() {
        assertThat(FeedbackPrompt.system())
                .contains("모범답안, 예시 답변")
                .contains("문장을 고쳐서 다시 써주기")
                .contains("총점이나 평균 계산");

        String user = FeedbackPrompt.user(context());
        assertThat(user)
                .contains("사람지향 소통인")
                .contains("공통 역량 분류: 의사소통능력")
                .contains("1점: 근거 불분명")
                .contains("5점: 이견 조율 확인");
    }

    @Test
    @DisplayName("인재상 미확인 기관이면 프롬프트가 그 사실을 알린다")
    void promptSaysWhenCompetenciesUnconfirmed() {
        FeedbackContext c = new FeedbackContext("금융감독원", false, "COVER_LETTER", null, "내용",
                context().criteria());
        assertThat(FeedbackPrompt.user(c)).contains("공기업 공통 역량 기준으로만 평가");
    }

    @Test
    @DisplayName("펜스나 군더더기가 붙어도 JSON을 뽑아낸다")
    void parsesFencedJson() {
        FeedbackResult r = FeedbackPrompt.parse(mapper, """
                다음은 결과입니다.
                ```json
                {"overall":"총평","verdicts":[{"canonicalId":"COMMUNICATION","score":4,
                "strengths":"드러남","gaps":"약함","quotedEvidence":"발췌"}]}
                ```
                """);
        assertThat(r.overall()).isEqualTo("총평");
        assertThat(r.verdicts()).singleElement().satisfies(v -> {
            assertThat(v.canonicalId()).isEqualTo("COMMUNICATION");
            assertThat(v.score()).isEqualTo(4);
        });
    }

    @Test
    @DisplayName("JSON이 아니면 502 — 못 읽은 응답을 그냥 넘기지 않는다")
    void rejectsNonJson() {
        assertThatThrownBy(() -> FeedbackPrompt.parse(mapper, "죄송하지만 답할 수 없습니다"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("Gemini 응답에서 텍스트를 뽑고, 내용이 없으면 이유와 함께 실패한다")
    void geminiExtraction() throws Exception {
        assertThat(GeminiLlmClient.extractText(mapper.readTree(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"a\\\":1}\"}]}}]}")))
                .isEqualTo("{\"a\":1}");

        assertThatThrownBy(() -> GeminiLlmClient.extractText(mapper.readTree(
                "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("SAFETY");
    }
}
