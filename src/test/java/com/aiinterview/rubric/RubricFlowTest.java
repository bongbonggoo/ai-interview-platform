package com.aiinterview.rubric;

import com.aiinterview.company.CompanyService;
import com.aiinterview.company.dto.CompanyCreateRequest;
import com.aiinterview.company.dto.CompanyResponse;
import com.aiinterview.competency.CompetencyLibraryService;
import com.aiinterview.competency.dto.CompetencyUpsertRequest;
import com.aiinterview.question.QuestionService;
import com.aiinterview.question.dto.QuestionCreateRequest;
import com.aiinterview.rubric.dto.EvaluationCriterionCreateRequest;
import com.aiinterview.rubric.dto.RubricValidationResponse;
import com.aiinterview.rubric.dto.RubricVersionResponse;
import com.aiinterview.rubric.dto.ScoreAnchorRequest;
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
 * CLAUDE.md의 "절대 지켜야 할 원칙"을 코드가 실제로 막고 있는지 고정해두는 테스트.
 * 원칙 2(역량은 재사용 자산), 원칙 3(루브릭은 덮어쓰지 않고 버전이 오른다)이 주 대상.
 */
@SpringBootTest
@Transactional
class RubricFlowTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired CompanyService companyService;
    @Autowired QuestionService questionService;
    @Autowired CompetencyLibraryService competencyLibraryService;
    @Autowired RubricVersionService rubricVersionService;
    @Autowired EvaluationCriterionService evaluationCriterionService;
    @Autowired ScoreAnchorService scoreAnchorService;

    private UUID companyId;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        CompanyResponse company = companyService.create(
                new CompanyCreateRequest("한국철도공사-" + SEQ.incrementAndGet(), "info.korail.com"));
        companyId = company.id();
        questionId = questionService.create(companyId,
                new QuestionCreateRequest("직무", "안전이 최우선인 상황에서 원칙과 효율이 충돌한 경험을 말해주세요", false)).id();

        competencyLibraryService.upsert("SAFETY_MINDSET",
                new CompetencyUpsertRequest("안전의식", "규정 준수와 위험 인지", "A"));
        competencyLibraryService.upsert("COMMUNICATION",
                new CompetencyUpsertRequest("의사소통", "상대에 맞춰 명확히 전달", "A"));
    }

    @Test
    @DisplayName("루브릭 버전은 회사별로 1부터 자동 증가한다")
    void versionNumberIncrements() {
        assertThat(rubricVersionService.create(companyId, null).versionNumber()).isEqualTo(1);
        assertThat(rubricVersionService.create(companyId, null).versionNumber()).isEqualTo(2);
        assertThat(rubricVersionService.create(companyId, null).versionNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("역량 라이브러리에 없는 canonical_id는 자동 생성되지 않고 404로 막힌다")
    void unknownCompetencyIsRejected() {
        UUID rubricVersionId = rubricVersionService.create(companyId, null).id();

        assertThatThrownBy(() -> evaluationCriterionService.create(rubricVersionId,
                new EvaluationCriterionCreateRequest(questionId, "코레일_고객지향_v2", 100, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("한 질문의 가중치 합이 100을 넘으면 거부된다")
    void weightSumCannotExceed100() {
        UUID rubricVersionId = rubricVersionService.create(companyId, null).id();
        evaluationCriterionService.create(rubricVersionId,
                new EvaluationCriterionCreateRequest(questionId, "SAFETY_MINDSET", 60, null));

        assertThatThrownBy(() -> evaluationCriterionService.create(rubricVersionId,
                new EvaluationCriterionCreateRequest(questionId, "COMMUNICATION", 50, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("가중치 합 100 + 1/3/5 앵커가 다 있어야 verified로 올라가고, 회사 status도 같이 오른다")
    void verifyRequiresFullWeightsAndAnchors() {
        UUID rubricVersionId = rubricVersionService.create(companyId, null).id();
        evaluationCriterionService.create(rubricVersionId,
                new EvaluationCriterionCreateRequest(questionId, "SAFETY_MINDSET", 60, anchors()));

        // 아직 합이 60 -> 막힌다
        RubricValidationResponse partial = rubricVersionService.validate(rubricVersionId);
        assertThat(partial.valid()).isFalse();
        assertThat(partial.problems()).anyMatch(p -> p.contains("weight_pct 합이 60"));
        assertThatThrownBy(() -> rubricVersionService.verify(rubricVersionId))
                .isInstanceOf(ResponseStatusException.class);

        evaluationCriterionService.create(rubricVersionId,
                new EvaluationCriterionCreateRequest(questionId, "COMMUNICATION", 40, anchors()));

        assertThat(rubricVersionService.validate(rubricVersionId).valid()).isTrue();
        RubricVersionResponse verified = rubricVersionService.verify(rubricVersionId);
        assertThat(verified.status()).isEqualTo("verified");
        assertThat(companyService.getById(companyId).status()).isEqualTo("verified");
    }

    @Test
    @DisplayName("앵커가 1/3/5 중 빠진 게 있으면 verified로 못 올린다")
    void verifyRequiresAllThreeAnchors() {
        UUID rubricVersionId = rubricVersionService.create(companyId, null).id();
        evaluationCriterionService.create(rubricVersionId, new EvaluationCriterionCreateRequest(
                questionId, "SAFETY_MINDSET", 100,
                List.of(new ScoreAnchorRequest(1, "규정을 인지하지 못함"), new ScoreAnchorRequest(5, "선제적으로 위험을 차단함"))));

        RubricValidationResponse report = rubricVersionService.validate(rubricVersionId);
        assertThat(report.valid()).isFalse();
        assertThat(report.problems()).anyMatch(p -> p.contains("1/3/5"));
    }

    @Test
    @DisplayName("verified된 버전은 더 이상 수정할 수 없다 — 새 버전을 만들어야 한다")
    void verifiedRubricIsImmutable() {
        UUID rubricVersionId = rubricVersionService.create(companyId, null).id();
        var criterion = evaluationCriterionService.create(rubricVersionId,
                new EvaluationCriterionCreateRequest(questionId, "SAFETY_MINDSET", 100, anchors()));
        rubricVersionService.verify(rubricVersionId);

        assertThatThrownBy(() -> scoreAnchorService.upsert(criterion.id(),
                new ScoreAnchorRequest(3, "문구를 몰래 고쳐본다")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // 새 버전을 만드는 길은 열려 있다
        assertThat(rubricVersionService.create(companyId, null).versionNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 level 앵커를 다시 넣으면 새로 생기지 않고 문구만 바뀐다")
    void anchorUpsertReplacesDescription() {
        UUID rubricVersionId = rubricVersionService.create(companyId, null).id();
        var criterion = evaluationCriterionService.create(rubricVersionId,
                new EvaluationCriterionCreateRequest(questionId, "SAFETY_MINDSET", 100, anchors()));

        scoreAnchorService.upsert(criterion.id(), new ScoreAnchorRequest(3, "고친 문구"));

        var stored = scoreAnchorService.getByCriterion(criterion.id());
        assertThat(stored).hasSize(3);
        assertThat(stored).filteredOn(a -> a.level() == 3).singleElement()
                .extracting(a -> a.description()).isEqualTo("고친 문구");
    }

    private static List<ScoreAnchorRequest> anchors() {
        return List.of(
                new ScoreAnchorRequest(1, "기준을 충족하지 못함"),
                new ScoreAnchorRequest(3, "기본 수준을 충족함"),
                new ScoreAnchorRequest(5, "기대를 넘어섬"));
    }
}
