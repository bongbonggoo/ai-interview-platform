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
import com.aiinterview.dto.response.InterviewDetailResponse;
import com.aiinterview.dto.response.InterviewSessionResponse;
import com.aiinterview.dto.response.InterviewTurnResponse;
import com.aiinterview.entity.Question;
import com.aiinterview.repository.QuestionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 면접 시작 -> 질문(턴) -> 답변 저장 플로우.
 * 채점기가 붙기 전에 "어떤 회사 / 어떤 루브릭 버전 / 어떤 질문 / 뭐라고 답했나"가
 * DB에 확실히 남는지를 여기서 못박아둔다.
 */
@SpringBootTest
@Transactional
class InterviewFlowTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired CompanyService companyService;
    @Autowired JobPositionService jobPositionService;
    @Autowired QuestionService questionService;
    @Autowired QuestionRepository questionRepository;
    @Autowired CompetencyLibraryService competencyLibraryService;
    @Autowired RubricVersionService rubricVersionService;
    @Autowired EvaluationCriterionService evaluationCriterionService;
    @Autowired InterviewSessionService interviewSessionService;
    @Autowired InterviewTurnService interviewTurnService;
    @Autowired InterviewAnswerService interviewAnswerService;

    private UUID companyId;
    private UUID jobPositionId;
    private UUID questionId;
    private UUID verifiedRubricId;

    @BeforeEach
    void setUp() {
        companyId = companyService.create(
                new CompanyCreateRequest("한국철도공사-" + SEQ.incrementAndGet(), "info.korail.com")).id();
        jobPositionId = jobPositionService.create(companyId, new JobPositionCreateRequest("사무영업")).id();
        questionId = questionService.create(companyId,
                new QuestionCreateRequest("직무", "고객이 불합리한 요구를 할 때 어떻게 대응하시겠습니까?", false)).id();

        competencyLibraryService.upsert("CUSTOMER_ORIENTATION",
                new CompetencyUpsertRequest("고객지향", "고객 관점에서 판단", "A"));

        verifiedRubricId = rubricVersionService.create(companyId, null).id();
        evaluationCriterionService.create(verifiedRubricId, new EvaluationCriterionCreateRequest(
                questionId, "CUSTOMER_ORIENTATION", 100, List.of(
                        new ScoreAnchorRequest(1, "고객 요구를 무시함"),
                        new ScoreAnchorRequest(3, "규정 내에서 대응함"),
                        new ScoreAnchorRequest(5, "대안을 제시해 갈등을 해소함"))));
        rubricVersionService.verify(verifiedRubricId);
    }

    private UUID startSession() {
        return interviewSessionService.create(new InterviewSessionCreateRequest(
                "user-1", companyId, jobPositionId, verifiedRubricId, "압박형")).sessionId();
    }

    @Test
    @DisplayName("면접 시작 -> 질문 -> 답변 제출 -> 조회가 한 흐름으로 이어진다")
    void fullFlow() {
        UUID sessionId = startSession();

        InterviewTurnResponse turn = interviewTurnService.create(sessionId,
                new InterviewTurnCreateRequest(questionId, null));
        // 생성 응답에서도 createdAt이 채워져 있어야 한다(flush 전 매핑하면 null로 나갔었다)
        assertThat(turn.createdAt()).isNotNull();
        interviewAnswerService.submit(sessionId,
                new InterviewAnswerCreateRequest(turn.turnId(), "먼저 고객의 요구사항을 정확히 파악하겠습니다", 62));

        InterviewDetailResponse detail = interviewSessionService.getDetail(sessionId);

        // 채점기가 필요로 하는 4가지가 다 있는가
        assertThat(detail.session().companyId()).isEqualTo(companyId);
        assertThat(detail.session().rubricVersionId()).isEqualTo(verifiedRubricId);
        assertThat(detail.turns()).hasSize(1);
        assertThat(detail.turns().get(0).turn().questionId()).isEqualTo(questionId);
        assertThat(detail.turns().get(0).answer().answerText()).startsWith("먼저 고객의 요구사항을");
        assertThat(detail.turns().get(0).answer().submittedAt()).isNotNull();
        assertThat(detail.session().status()).isEqualTo("IN_PROGRESS");

        assertThat(interviewSessionService.end(sessionId).status()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("verified되지 않은 루브릭으로는 면접을 시작할 수 없다")
    void cannotStartWithDraftRubric() {
        UUID draftRubricId = rubricVersionService.create(companyId, null).id();

        assertThatThrownBy(() -> interviewSessionService.create(new InterviewSessionCreateRequest(
                "user-1", companyId, jobPositionId, draftRubricId, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("다른 회사의 직무나 루브릭은 붙일 수 없다")
    void rejectsCrossCompanyReferences() {
        UUID otherCompanyId = companyService.create(
                new CompanyCreateRequest("다른공사-" + SEQ.incrementAndGet(), null)).id();
        UUID otherJobPositionId = jobPositionService.create(otherCompanyId, new JobPositionCreateRequest("토목")).id();

        assertThatThrownBy(() -> interviewSessionService.create(new InterviewSessionCreateRequest(
                "user-1", companyId, otherJobPositionId, verifiedRubricId, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("세션은 시작 시점의 루브릭 버전을 고정한다 — 이후 새 버전이 나와도 안 바뀐다")
    void sessionPinsRubricVersion() {
        UUID sessionId = startSession();
        UUID newerRubricId = rubricVersionService.create(companyId, null).id();

        assertThat(newerRubricId).isNotEqualTo(verifiedRubricId);
        assertThat(interviewSessionService.getById(sessionId).rubricVersionId()).isEqualTo(verifiedRubricId);
        assertThat(interviewSessionService.getById(sessionId).rubricVersionNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("질문 문구 스냅샷은 Question 원본이 바뀌어도 그대로 남는다")
    void questionSnapshotIsImmutable() {
        UUID sessionId = startSession();
        InterviewTurnResponse turn = interviewTurnService.create(sessionId,
                new InterviewTurnCreateRequest(questionId, null));
        String askedText = turn.questionText();

        Question question = questionRepository.findById(questionId).orElseThrow();
        question.setQuestionText("나중에 고쳐진 질문 문구");
        questionRepository.saveAndFlush(question);

        assertThat(interviewTurnService.getBySession(sessionId).get(0).questionText()).isEqualTo(askedText);
        assertThat(askedText).doesNotContain("나중에 고쳐진");
    }

    @Test
    @DisplayName("questionId 없는 턴은 꼬리질문으로 저장되고, 문구가 없으면 거부된다")
    void followUpTurn() {
        UUID sessionId = startSession();

        InterviewTurnResponse followUp = interviewTurnService.create(sessionId,
                new InterviewTurnCreateRequest(null, "그 상황에서 상사가 반대했다면요?"));
        assertThat(followUp.turnType()).isEqualTo("FOLLOW_UP");
        assertThat(followUp.questionId()).isNull();

        assertThatThrownBy(() -> interviewTurnService.create(sessionId, new InterviewTurnCreateRequest(null, "  ")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("orderIndex는 세션별로 1부터 자동 증가한다")
    void orderIndexIncrements() {
        UUID sessionId = startSession();

        assertThat(interviewTurnService.create(sessionId, new InterviewTurnCreateRequest(questionId, null)).orderIndex())
                .isEqualTo(1);
        assertThat(interviewTurnService.create(sessionId, new InterviewTurnCreateRequest(null, "꼬리질문")).orderIndex())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("한 턴에 답변은 하나뿐 — 재제출은 덮어쓰지 않고 409")
    void answerIsSubmittedOnce() {
        UUID sessionId = startSession();
        InterviewTurnResponse turn = interviewTurnService.create(sessionId,
                new InterviewTurnCreateRequest(questionId, null));
        interviewAnswerService.submit(sessionId, new InterviewAnswerCreateRequest(turn.turnId(), "첫 답변", 30));

        assertThatThrownBy(() -> interviewAnswerService.submit(sessionId,
                new InterviewAnswerCreateRequest(turn.turnId(), "몰래 바꾼 답변", 30)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("다른 세션의 턴에는 답변할 수 없다")
    void cannotAnswerTurnOfAnotherSession() {
        UUID sessionA = startSession();
        UUID sessionB = startSession();
        InterviewTurnResponse turnOfA = interviewTurnService.create(sessionA,
                new InterviewTurnCreateRequest(questionId, null));

        assertThatThrownBy(() -> interviewAnswerService.submit(sessionB,
                new InterviewAnswerCreateRequest(turnOfA.turnId(), "남의 턴에 답변", 10)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("종료된 세션에는 턴도 답변도 더 붙지 않는다")
    void endedSessionIsClosed() {
        UUID sessionId = startSession();
        InterviewTurnResponse turn = interviewTurnService.create(sessionId,
                new InterviewTurnCreateRequest(questionId, null));
        interviewSessionService.end(sessionId);

        assertThatThrownBy(() -> interviewTurnService.create(sessionId, new InterviewTurnCreateRequest(null, "추가 질문")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThatThrownBy(() -> interviewAnswerService.submit(sessionId,
                new InterviewAnswerCreateRequest(turn.turnId(), "늦은 답변", 10)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("end는 두 번 불러도 안전하다")
    void endIsIdempotent() {
        UUID sessionId = startSession();
        InterviewSessionResponse first = interviewSessionService.end(sessionId);
        InterviewSessionResponse second = interviewSessionService.end(sessionId);

        assertThat(second.endedAt()).isEqualTo(first.endedAt());
    }
}
