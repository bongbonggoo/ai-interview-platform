package com.aiinterview.service;

import com.aiinterview.dto.request.CompanyCreateRequest;
import com.aiinterview.dto.request.CompetencyUpsertRequest;
import com.aiinterview.dto.request.EvaluationCriterionCreateRequest;
import com.aiinterview.dto.request.InterviewAnswerCreateRequest;
import com.aiinterview.dto.request.InterviewSessionCreateRequest;
import com.aiinterview.dto.request.InterviewTurnCreateRequest;
import com.aiinterview.dto.request.JobPositionCreateRequest;
import com.aiinterview.dto.request.QuestionCreateRequest;
import com.aiinterview.dto.request.ScoreAnchorRequest;
import com.aiinterview.dto.response.AnswerEvaluationResponse;
import com.aiinterview.dto.response.InterviewResultResponse;
import com.aiinterview.dto.response.InterviewTurnResponse;
import com.aiinterview.service.scorer.AnswerScorer;
import com.aiinterview.service.scorer.CriterionScore;
import com.aiinterview.service.scorer.ScoringRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 채점 파이프라인. 실제 Claude 호출 없이, 채점기가 무엇을 내놓든 서비스가 계약을 강제하는지를 본다.
 * 핵심: AI가 낸 것은 항목 점수뿐이고 총점은 코드가 계산한다(CLAUDE.md 원칙 1).
 */
@SpringBootTest
@Transactional
class EvaluationFlowTest {

    /** 테스트가 원하는 점수를 내도록 갈아끼울 수 있는 채점기. */
    static class ProgrammableScorer implements AnswerScorer {
        Function<ScoringRequest, List<CriterionScore>> behaviour =
                req -> req.criteria().stream()
                        .map(c -> new CriterionScore(c.criterionId(), 4, "근거"))
                        .toList();
        ScoringRequest lastRequest;

        @Override public List<CriterionScore> score(ScoringRequest request) {
            lastRequest = request;
            return behaviour.apply(request);
        }
        @Override public String name() { return "programmable"; }
    }

    @TestConfiguration
    static class Config {
        @Bean @Primary ProgrammableScorer programmableScorer() { return new ProgrammableScorer(); }
    }

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired ProgrammableScorer scorer;
    @Autowired CompanyService companyService;
    @Autowired JobPositionService jobPositionService;
    @Autowired QuestionService questionService;
    @Autowired CompetencyLibraryService competencyLibraryService;
    @Autowired RubricVersionService rubricVersionService;
    @Autowired EvaluationCriterionService evaluationCriterionService;
    @Autowired InterviewSessionService interviewSessionService;
    @Autowired InterviewTurnService interviewTurnService;
    @Autowired InterviewAnswerService interviewAnswerService;
    @Autowired EvaluationService evaluationService;

    private UUID sessionId;
    private UUID turnId;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        scorer.behaviour = req -> req.criteria().stream()
                .map(c -> new CriterionScore(c.criterionId(), 4, "근거"))
                .toList();

        UUID companyId = companyService.create(
                new CompanyCreateRequest("한국철도공사-" + SEQ.incrementAndGet(), null)).id();
        UUID jobPositionId = jobPositionService.create(companyId, new JobPositionCreateRequest("사무영업")).id();
        questionId = questionService.create(companyId,
                new QuestionCreateRequest("직무", "고객이 불합리한 요구를 할 때 어떻게 대응하시겠습니까?", false)).id();

        competencyLibraryService.upsert("CUSTOMER_ORIENTATION",
                new CompetencyUpsertRequest("고객지향", "고객 관점에서 판단", "A"));
        competencyLibraryService.upsert("PROBLEM_SOLVING",
                new CompetencyUpsertRequest("문제해결력", "원인을 짚고 대안을 제시", "A"));

        UUID rubricId = rubricVersionService.create(companyId, null).id();
        evaluationCriterionService.create(rubricId, new EvaluationCriterionCreateRequest(
                questionId, "CUSTOMER_ORIENTATION", 60, anchors("고객 요구 무시", "규정 내 대응", "대안 제시")));
        evaluationCriterionService.create(rubricId, new EvaluationCriterionCreateRequest(
                questionId, "PROBLEM_SOLVING", 40, anchors("원인 미파악", "원인 파악", "대안까지 제시")));
        rubricVersionService.verify(rubricId);

        sessionId = interviewSessionService.create(new InterviewSessionCreateRequest(
                "user-1", companyId, jobPositionId, rubricId, null)).sessionId();
        InterviewTurnResponse turn = interviewTurnService.create(sessionId,
                new InterviewTurnCreateRequest(questionId, null));
        turnId = turn.turnId();
        interviewAnswerService.submit(sessionId, new InterviewAnswerCreateRequest(
                turnId, "먼저 요구사항을 파악하고, 규정상 불가한 부분은 대안을 제시하겠습니다", 62));
    }

    private static List<ScoreAnchorRequest> anchors(String one, String three, String five) {
        return List.of(new ScoreAnchorRequest(1, one), new ScoreAnchorRequest(3, three),
                new ScoreAnchorRequest(5, five));
    }

    @Test
    @DisplayName("채점하면 criterion마다 결과가 저장되고, 총점은 Σ(점수/5 × 가중치)로 계산된다")
    void scoresAndComputesTotal() {
        scorer.behaviour = req -> req.criteria().stream()
                .map(c -> new CriterionScore(c.criterionId(),
                        "CUSTOMER_ORIENTATION".equals(c.canonicalId()) ? 4 : 2, "근거"))
                .toList();

        AnswerEvaluationResponse result = evaluationService.evaluateTurn(sessionId, turnId);

        assertThat(result.criterionScores()).hasSize(2);
        assertThat(result.scoredBy()).isEqualTo("programmable");
        // 4/5×60 + 2/5×40 = 48 + 16
        assertThat(result.questionScore()).isEqualTo(64.0);
    }

    @Test
    @DisplayName("프롬프트에는 이 세션에 고정된 루브릭의 기준과 1/3/5 앵커만 들어간다")
    void promptCarriesSessionRubricOnly() {
        evaluationService.evaluateTurn(sessionId, turnId);

        ScoringRequest sent = scorer.lastRequest;
        assertThat(sent.criteria()).hasSize(2);
        assertThat(sent.answerText()).startsWith("먼저 요구사항을");
        assertThat(sent.criteria()).allSatisfy(c -> {
            assertThat(c.anchorLevel1()).isNotBlank();
            assertThat(c.anchorLevel3()).isNotBlank();
            assertThat(c.anchorLevel5()).isNotBlank();
        });
        assertThat(sent.criteria()).extracting(ScoringRequest.Criterion::weightPct)
                .containsExactlyInAnyOrder(60, 40);
    }

    @Test
    @DisplayName("1~5 밖의 점수는 저장하지 않고 502로 막는다")
    void rejectsOutOfRangeScore() {
        scorer.behaviour = req -> req.criteria().stream()
                .map(c -> new CriterionScore(c.criterionId(), 9, "제멋대로"))
                .toList();

        assertThatThrownBy(() -> evaluationService.evaluateTurn(sessionId, turnId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        // 저장까지 갔다가 실패한 게 아니라, 아무것도 안 남아야 한다
        assertThat(evaluationService.getResult(sessionId).scoredAnswerCount()).isZero();
    }

    @Test
    @DisplayName("기준 하나를 빼먹은 채점 결과는 거부된다")
    void rejectsMissingCriterion() {
        scorer.behaviour = req -> List.of(
                new CriterionScore(req.criteria().get(0).criterionId(), 4, "하나만"));

        assertThatThrownBy(() -> evaluationService.evaluateTurn(sessionId, turnId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("이미 채점된 답변은 다시 채점하지 않는다")
    void rejectsDoubleScoring() {
        evaluationService.evaluateTurn(sessionId, turnId);

        assertThatThrownBy(() -> evaluationService.evaluateTurn(sessionId, turnId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("답변이 없는 턴과 꼬리질문 턴은 채점 대상이 아니다")
    void rejectsUnscorableTurns() {
        InterviewTurnResponse unanswered = interviewTurnService.create(sessionId,
                new InterviewTurnCreateRequest(questionId, null));
        assertThatThrownBy(() -> evaluationService.evaluateTurn(sessionId, unanswered.turnId()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        InterviewTurnResponse followUp = interviewTurnService.create(sessionId,
                new InterviewTurnCreateRequest(null, "그럼 상사가 반대하면요?"));
        interviewAnswerService.submit(sessionId,
                new InterviewAnswerCreateRequest(followUp.turnId(), "설득하겠습니다", 20));
        assertThatThrownBy(() -> evaluationService.evaluateTurn(sessionId, followUp.turnId()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("세션 일괄 채점은 꼬리질문과 이미 채점된 답변을 건너뛴다")
    void evaluateSessionSkipsWhatItShould() {
        InterviewTurnResponse followUp = interviewTurnService.create(sessionId,
                new InterviewTurnCreateRequest(null, "꼬리질문"));
        interviewAnswerService.submit(sessionId,
                new InterviewAnswerCreateRequest(followUp.turnId(), "답변", 10));

        assertThat(evaluationService.evaluateSession(sessionId)).hasSize(1);
        assertThat(evaluationService.evaluateSession(sessionId)).isEmpty();
    }

    @Test
    @DisplayName("결과 조회: 근거가 남고, 총점은 저장값이 아니라 매번 계산된다")
    void resultView() {
        evaluationService.evaluateTurn(sessionId, turnId);

        InterviewResultResponse result = evaluationService.getResult(sessionId);
        assertThat(result.scoredAnswerCount()).isEqualTo(1);
        assertThat(result.unscoredAnswerCount()).isZero();
        assertThat(result.totalScore()).isEqualTo(80.0); // 전부 4점 -> 4/5×100
        assertThat(result.answers()).singleElement().satisfies(answer -> {
            assertThat(answer.criterionScores()).extracting(c -> c.canonicalId())
                    .containsExactly("CUSTOMER_ORIENTATION", "PROBLEM_SOLVING");
            assertThat(answer.criterionScores()).allSatisfy(c -> assertThat(c.evidenceText()).isNotBlank());
            // 저장된 것은 항목 점수뿐 — scoredBy는 조회 시점에 알 수 없다
            assertThat(answer.scoredBy()).isNull();
        });
    }
}
