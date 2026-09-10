package com.aiinterview.service;

import com.aiinterview.dto.request.FeedbackRequest;
import com.aiinterview.dto.response.FeedbackResponse;
import com.aiinterview.service.scorer.CompetencyVerdict;
import com.aiinterview.service.scorer.FeedbackContext;
import com.aiinterview.service.scorer.FeedbackGenerator;
import com.aiinterview.service.scorer.FeedbackResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 제품의 본체: 기관 고르고 글 붙여넣으면 그 기관 인재상 기준으로 피드백이 나온다.
 * 여기서 못박는 것 — 기준이 그 회사 인재상에서 나오는가, 총점을 코드가 계산하는가,
 * 근거 없는 기관은 공통 기준으로 떨어지는가.
 */
@SpringBootTest
class FeedbackFlowTest {

    static class ProgrammableFeedback implements FeedbackGenerator {
        Function<FeedbackContext, FeedbackResult> behaviour = ctx -> new FeedbackResult(
                "총평", ctx.criteria().stream()
                        .map(c -> new CompetencyVerdict(c.canonicalId(), 4, "잘 드러남", "보완할 점", "원문 발췌"))
                        .toList());
        FeedbackContext last;

        @Override public FeedbackResult generate(FeedbackContext context) {
            last = context;
            return behaviour.apply(context);
        }
        @Override public String name() { return "programmable"; }
    }

    @TestConfiguration
    static class Config {
        @Bean @Primary ProgrammableFeedback programmableFeedback() { return new ProgrammableFeedback(); }
    }

    private static final String SAMPLE = """
            고객센터 아르바이트를 하며 환불 기한이 지난 고객의 항의를 받은 적이 있습니다.
            규정상 환불은 불가했지만, 고객이 실제로 원하는 것이 금액이 아니라 제품 하자에 대한
            인정이라는 점을 파악했습니다. 교환 절차를 안내하고 품질팀에 사례를 전달해
            동일 문의가 반복되지 않도록 응대 지침을 수정하도록 건의했습니다.
            """;

    @Autowired ProgrammableFeedback generator;
    @Autowired FeedbackService feedbackService;

    @BeforeEach
    void resetBehaviour() {
        generator.behaviour = ctx -> new FeedbackResult(
                "총평", ctx.criteria().stream()
                        .map(c -> new CompetencyVerdict(c.canonicalId(), 4, "잘 드러남", "보완할 점", "원문 발췌"))
                        .toList());
    }

    @Test
    @DisplayName("시드된 9개 기관이 목록에 있고, 별칭으로도 찾힌다")
    void seededCompanies() {
        assertThat(feedbackService.selectableCompanies()).extracting(c -> c.name())
                .contains("한국철도공사", "한국전력공사", "한국수력원자력", "건강보험심사평가원",
                        "국민건강보험공단", "부산항만공사", "대한무역투자진흥공사",
                        "도로교통공단", "금융감독원");

        // "코레일"로 요청해도 한국철도공사로 찾아간다
        FeedbackResponse res = feedbackService.giveFeedback(
                new FeedbackRequest("코레일", "COVER_LETTER", "지원동기", SAMPLE));
        assertThat(res.companyName()).isEqualTo("한국철도공사");
    }

    @Test
    @DisplayName("평가 기준이 그 기관의 인재상에서 나오고, 출처가 응답에 함께 실린다")
    void criteriaComeFromCompanyCompetencies() {
        FeedbackResponse res = feedbackService.giveFeedback(
                new FeedbackRequest("한국철도공사", "COVER_LETTER", "지원동기", SAMPLE));

        assertThat(res.competencyConfirmed()).isTrue();
        assertThat(res.sourceUrl()).contains("korail.com");
        assertThat(res.competencies()).extracting(c -> c.companyLabel())
                .containsExactlyInAnyOrder("사람지향 소통인", "고객지향 전문인", "미래지향 혁신인");

        // 프롬프트에 1/3/5 행동기준이 실제로 들어갔는가
        assertThat(generator.last.criteria()).allSatisfy(c -> {
            assertThat(c.anchor1()).isNotBlank();
            assertThat(c.anchor3()).isNotBlank();
            assertThat(c.anchor5()).isNotBlank();
        });
    }

    @Test
    @DisplayName("인재상을 확보 못 한 기관은 공통 기준으로 평가하고, 그 사실을 드러낸다")
    void unconfirmedCompanyFallsBackToCommonCriteria() {
        FeedbackResponse res = feedbackService.giveFeedback(
                new FeedbackRequest("금감원", "COVER_LETTER", null, SAMPLE));

        assertThat(res.companyName()).isEqualTo("금융감독원");
        assertThat(res.competencyConfirmed()).isFalse();
        assertThat(res.sourceUrl()).isNull();
        assertThat(res.competencies()).extracting(c -> c.canonicalId())
                .containsExactly("COMMUNICATION", "PROBLEM_SOLVING", "INTERPERSONAL",
                        "ORGANIZATIONAL_UNDERSTANDING", "WORK_ETHIC");
        assertThat(generator.last.competencyConfirmed()).isFalse();
    }

    @Test
    @DisplayName("총점은 AI가 아니라 코드가 계산한다")
    void totalScoreIsComputed() {
        generator.behaviour = ctx -> new FeedbackResult("총평", List.of(
                new CompetencyVerdict(ctx.criteria().get(0).canonicalId(), 5, "s", "g", "q"),
                new CompetencyVerdict(ctx.criteria().get(1).canonicalId(), 3, "s", "g", "q"),
                new CompetencyVerdict(ctx.criteria().get(2).canonicalId(), 1, "s", "g", "q")));

        FeedbackResponse res = feedbackService.giveFeedback(
                new FeedbackRequest("한국철도공사", "COVER_LETTER", null, SAMPLE));

        // (5+3+1)/3 = 3.0 -> 3/5 × 100
        assertThat(res.totalScore()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("점수에 해당하는 행동기준이 응답에 실린다 — 사용자가 왜 그 점수인지 볼 수 있어야 한다")
    void scoreCriterionIsExposed() {
        generator.behaviour = ctx -> new FeedbackResult("총평", ctx.criteria().stream()
                .map(c -> new CompetencyVerdict(c.canonicalId(), 3, "s", "g", "q")).toList());

        FeedbackResponse res = feedbackService.giveFeedback(
                new FeedbackRequest("한국철도공사", "COVER_LETTER", null, SAMPLE));

        assertThat(res.competencies()).allSatisfy(c ->
                assertThat(c.scoreCriterion()).isNotBlank());
    }

    @Test
    @DisplayName("1~5 밖의 점수나 빠진 역량은 응답으로 내보내지 않는다")
    void rejectsBrokenGeneratorOutput() {
        generator.behaviour = ctx -> new FeedbackResult("총평", ctx.criteria().stream()
                .map(c -> new CompetencyVerdict(c.canonicalId(), 9, "s", "g", "q")).toList());
        assertThatThrownBy(() -> feedbackService.giveFeedback(
                new FeedbackRequest("한국철도공사", "COVER_LETTER", null, SAMPLE)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        generator.behaviour = ctx -> new FeedbackResult("총평", List.of(
                new CompetencyVerdict(ctx.criteria().get(0).canonicalId(), 4, "s", "g", "q")));
        assertThatThrownBy(() -> feedbackService.giveFeedback(
                new FeedbackRequest("한국철도공사", "COVER_LETTER", null, SAMPLE)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("등록되지 않은 기관은 404")
    void unknownCompany() {
        assertThatThrownBy(() -> feedbackService.giveFeedback(
                new FeedbackRequest("없는공사", "COVER_LETTER", null, SAMPLE)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
