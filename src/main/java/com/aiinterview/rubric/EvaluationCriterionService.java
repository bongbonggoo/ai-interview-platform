package com.aiinterview.rubric;

import com.aiinterview.competency.CompetencyLibrary;
import com.aiinterview.competency.CompetencyLibraryService;
import com.aiinterview.question.Question;
import com.aiinterview.question.QuestionService;
import com.aiinterview.rubric.dto.EvaluationCriterionCreateRequest;
import com.aiinterview.rubric.dto.EvaluationCriterionResponse;
import com.aiinterview.rubric.dto.ScoreAnchorRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * 가중치 규칙이 사는 곳.
 * 한 질문에 딸린 criterion들의 weight_pct 합은 100을 넘을 수 없고, verified 시점에 정확히 100이어야 한다
 * (쌓아가는 중에는 100 미만이 정상이라, "정확히 100" 검사는 RubricVersionService.verify가 맡는다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationCriterionService {

    private static final int MAX_WEIGHT_SUM = 100;

    private final EvaluationCriterionRepository evaluationCriterionRepository;
    private final ScoreAnchorRepository scoreAnchorRepository;
    private final RubricVersionService rubricVersionService;
    private final QuestionService questionService;
    private final CompetencyLibraryService competencyLibraryService;
    private final ScoreAnchorService scoreAnchorService;

    @Transactional
    public EvaluationCriterionResponse create(UUID rubricVersionId, EvaluationCriterionCreateRequest request) {
        RubricVersion rubricVersion = rubricVersionService.getEntity(rubricVersionId);
        rubricVersionService.assertMutable(rubricVersion);

        Question question = questionService.getEntity(request.questionId());
        if (!question.getCompany().getId().equals(rubricVersion.getCompany().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "질문이 이 루브릭의 회사 소속이 아닙니다 (질문 회사=" + question.getCompany().getId()
                            + ", 루브릭 회사=" + rubricVersion.getCompany().getId() + ")");
        }

        // 원칙 2: 없는 canonical_id면 여기서 만들지 않고 404로 막는다.
        CompetencyLibrary competency = competencyLibraryService.getEntity(request.canonicalId());

        if (evaluationCriterionRepository.existsByRubricVersionIdAndQuestionIdAndCompetencyCanonicalId(
                rubricVersionId, question.getId(), competency.getCanonicalId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이 질문에 같은 역량(" + competency.getCanonicalId() + ")이 이미 등록돼 있습니다");
        }

        int currentSum = currentWeightSum(rubricVersionId, question.getId());
        if (currentSum + request.weightPct() > MAX_WEIGHT_SUM) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "가중치 합이 100을 넘습니다: 현재 " + currentSum + " + 요청 " + request.weightPct());
        }

        EvaluationCriterion criterion = evaluationCriterionRepository.save(EvaluationCriterion.builder()
                .rubricVersion(rubricVersion)
                .question(question)
                .competency(competency)
                .weightPct(request.weightPct())
                .build());

        if (request.anchors() != null) {
            for (ScoreAnchorRequest anchor : request.anchors()) {
                scoreAnchorService.upsert(criterion, anchor);
            }
        }

        return toResponse(criterion);
    }

    public List<EvaluationCriterionResponse> getByRubricVersion(UUID rubricVersionId) {
        rubricVersionService.getEntity(rubricVersionId);
        return evaluationCriterionRepository.findByRubricVersionId(rubricVersionId).stream()
                .map(this::toResponse)
                .toList();
    }

    public EvaluationCriterionResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    public EvaluationCriterion getEntity(UUID id) {
        return evaluationCriterionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "평가 기준을 찾을 수 없습니다: " + id));
    }

    int currentWeightSum(UUID rubricVersionId, UUID questionId) {
        return evaluationCriterionRepository.findByRubricVersionIdAndQuestionId(rubricVersionId, questionId).stream()
                .mapToInt(EvaluationCriterion::getWeightPct)
                .sum();
    }

    private EvaluationCriterionResponse toResponse(EvaluationCriterion criterion) {
        return EvaluationCriterionResponse.from(
                criterion, scoreAnchorRepository.findByCriterionIdOrderByLevelAsc(criterion.getId()));
    }
}
