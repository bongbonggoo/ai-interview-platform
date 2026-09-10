package com.aiinterview.service;

import com.aiinterview.dto.response.AnswerEvaluationResponse;
import com.aiinterview.dto.response.CriterionScoreResponse;
import com.aiinterview.dto.response.InterviewResultResponse;
import com.aiinterview.dto.response.InterviewSessionResponse;
import com.aiinterview.entity.EvaluationCriterion;
import com.aiinterview.entity.EvaluationResult;
import com.aiinterview.entity.InterviewAnswer;
import com.aiinterview.entity.InterviewSession;
import com.aiinterview.entity.InterviewTurn;
import com.aiinterview.entity.Question;
import com.aiinterview.entity.ScoreAnchor;
import com.aiinterview.repository.EvaluationCriterionRepository;
import com.aiinterview.repository.EvaluationResultRepository;
import com.aiinterview.repository.InterviewAnswerRepository;
import com.aiinterview.repository.InterviewTurnRepository;
import com.aiinterview.repository.ScoreAnchorRepository;
import com.aiinterview.service.scorer.AnswerScorer;
import com.aiinterview.service.scorer.CriterionScore;
import com.aiinterview.service.scorer.ScoringRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 채점 파이프라인: InterviewAnswer -> (세션에 고정된 루브릭의 criterion + 앵커) -> AI -> EvaluationResult.
 *
 * 두 가지를 이 서비스가 책임진다.
 * 1. AI에게 넘기는 기준은 반드시 그 세션의 rubricVersion에서 나온 것만이다.
 * 2. AI가 낸 것은 항목별 1~5점뿐이고, 총점은 ScoreCalculator가 계산한다(원칙 1).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationService {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;

    private final AnswerScorer answerScorer;
    private final EvaluationResultRepository evaluationResultRepository;
    private final EvaluationCriterionRepository evaluationCriterionRepository;
    private final ScoreAnchorRepository scoreAnchorRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewTurnRepository interviewTurnRepository;
    private final InterviewSessionService interviewSessionService;

    @Transactional
    public AnswerEvaluationResponse evaluateTurn(UUID sessionId, UUID turnId) {
        InterviewSession session = interviewSessionService.getEntity(sessionId);
        InterviewTurn turn = interviewTurnRepository.findById(turnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "턴을 찾을 수 없습니다: " + turnId));
        if (!turn.getSession().getId().equals(sessionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "턴 " + turnId + "은 세션 " + sessionId + "의 턴이 아닙니다");
        }
        return evaluate(session, turn);
    }

    /** 아직 채점 안 된 답변을 한꺼번에. 꼬리질문(원본 Question 없음)은 루브릭 기준이 없어 건너뛴다. */
    @Transactional
    public List<AnswerEvaluationResponse> evaluateSession(UUID sessionId) {
        InterviewSession session = interviewSessionService.getEntity(sessionId);

        List<AnswerEvaluationResponse> evaluated = new ArrayList<>();
        for (InterviewTurn turn : interviewTurnRepository.findBySessionIdOrderByOrderIndexAsc(sessionId)) {
            if (turn.getQuestion() == null) continue;
            InterviewAnswer answer = interviewAnswerRepository.findByTurnId(turn.getId()).orElse(null);
            if (answer == null || evaluationResultRepository.existsByAnswerId(answer.getId())) continue;
            evaluated.add(evaluate(session, turn));
        }
        return evaluated;
    }

    private AnswerEvaluationResponse evaluate(InterviewSession session, InterviewTurn turn) {
        InterviewAnswer answer = interviewAnswerRepository.findByTurnId(turn.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "아직 답변이 제출되지 않은 턴입니다: " + turn.getId()));

        // 이미 채점된 답변을 다시 채점하면 어느 결과가 유효한지 알 수 없게 된다.
        if (evaluationResultRepository.existsByAnswerId(answer.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 채점된 답변입니다: " + answer.getId());
        }

        if (turn.getQuestion() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "즉석 꼬리질문은 루브릭에 평가 기준이 없어 채점할 수 없습니다: 턴 " + turn.getId());
        }

        // 반드시 이 세션에 고정된 루브릭 버전에서 기준을 꺼낸다.
        List<EvaluationCriterion> criteria = evaluationCriterionRepository
                .findByRubricVersionIdAndQuestionId(session.getRubricVersion().getId(), turn.getQuestion().getId());
        if (criteria.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "루브릭 v" + session.getRubricVersion().getVersionNumber()
                            + "에 이 질문의 평가 기준이 없습니다: 질문 " + turn.getQuestion().getId());
        }

        List<CriterionScore> scores = answerScorer.score(buildScoringRequest(session, turn, answer, criteria));
        Map<UUID, CriterionScore> scoreByCriterionId = validateAndIndex(scores, criteria);

        List<EvaluationResult> saved = criteria.stream()
                .map(criterion -> evaluationResultRepository.save(EvaluationResult.builder()
                        .answer(answer)
                        .criterion(criterion)
                        .aiScore(scoreByCriterionId.get(criterion.getId()).score())
                        .evidenceText(scoreByCriterionId.get(criterion.getId()).evidence())
                        .build()))
                .toList();

        return toResponse(turn, answer, saved, answerScorer.name());
    }

    private ScoringRequest buildScoringRequest(InterviewSession session, InterviewTurn turn,
                                               InterviewAnswer answer, List<EvaluationCriterion> criteria) {
        List<ScoringRequest.Criterion> promptCriteria = criteria.stream()
                .map(criterion -> {
                    Map<Integer, String> anchors = scoreAnchorRepository
                            .findByCriterionIdOrderByLevelAsc(criterion.getId()).stream()
                            .collect(Collectors.toMap(ScoreAnchor::getLevel, ScoreAnchor::getDescription));
                    return new ScoringRequest.Criterion(
                            criterion.getId(),
                            criterion.getCompetency().getCanonicalId(),
                            criterion.getCompetency().getLabel(),
                            criterion.getCompetency().getDefinition(),
                            criterion.getWeightPct(),
                            anchors.get(1), anchors.get(3), anchors.get(5));
                })
                .toList();

        return new ScoringRequest(
                session.getCompany().getName(),
                session.getJobPosition().getName(),
                turn.getQuestionTextSnapshot(),
                answer.getAnswerText(),
                promptCriteria);
    }

    /** 채점기가 무엇이든(스텁이든 Claude든) 계약을 지켰는지 여기서 확인하고 나서야 저장한다. */
    private Map<UUID, CriterionScore> validateAndIndex(List<CriterionScore> scores,
                                                       List<EvaluationCriterion> criteria) {
        if (scores == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "채점 결과가 비어 있습니다");
        }

        Map<UUID, CriterionScore> byId = new LinkedHashMap<>();
        for (CriterionScore score : scores) {
            if (score.score() == null || score.score() < MIN_SCORE || score.score() > MAX_SCORE) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "점수는 1~5여야 합니다. 받은 값: " + score.score() + " (criterion " + score.criterionId() + ")");
            }
            if (byId.put(score.criterionId(), score) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "같은 평가 기준에 점수가 두 번 왔습니다: " + score.criterionId());
            }
        }

        List<UUID> missing = criteria.stream()
                .map(EvaluationCriterion::getId)
                .filter(id -> !byId.containsKey(id))
                .toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "채점되지 않은 평가 기준이 있습니다: " + missing);
        }
        if (byId.size() != criteria.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "루브릭에 없는 평가 기준에 점수가 왔습니다");
        }
        return byId;
    }

    public InterviewResultResponse getResult(UUID sessionId) {
        InterviewSession session = interviewSessionService.getEntity(sessionId);

        Map<UUID, List<EvaluationResult>> resultsByAnswerId =
                evaluationResultRepository.findByAnswerTurnSessionId(sessionId).stream()
                        .collect(Collectors.groupingBy(result -> result.getAnswer().getId(),
                                LinkedHashMap::new, Collectors.toList()));

        List<AnswerEvaluationResponse> answers = new ArrayList<>();
        List<Double> questionScores = new ArrayList<>();
        int unscored = 0;

        for (InterviewTurn turn : interviewTurnRepository.findBySessionIdOrderByOrderIndexAsc(sessionId)) {
            InterviewAnswer answer = interviewAnswerRepository.findByTurnId(turn.getId()).orElse(null);
            if (answer == null) continue;

            List<EvaluationResult> results = resultsByAnswerId.get(answer.getId());
            if (results == null || results.isEmpty()) {
                unscored++;
                continue;
            }
            AnswerEvaluationResponse response = toResponse(turn, answer, results, null);
            answers.add(response);
            questionScores.add(response.questionScore());
        }

        return new InterviewResultResponse(
                InterviewSessionResponse.from(session),
                ScoreCalculator.sessionScore(questionScores),
                answers.size(),
                unscored,
                answers);
    }

    /**
     * scoredBy는 채점 직후 응답에만 채운다 — 어떤 채점기가 매겼는지는 저장하지 않기 때문에
     * (ERD에 컬럼이 없다) 나중에 조회할 때는 알 수 없다. 필요해지면 컬럼을 추가할 것.
     */
    private AnswerEvaluationResponse toResponse(InterviewTurn turn, InterviewAnswer answer,
                                                List<EvaluationResult> results, String scoredBy) {
        List<ScoreCalculator.Weighted> weighted = results.stream()
                .map(result -> new ScoreCalculator.Weighted(
                        result.getAiScore(), result.getCriterion().getWeightPct()))
                .toList();

        return new AnswerEvaluationResponse(
                answer.getId(),
                turn.getId(),
                turn.getQuestionTextSnapshot(),
                scoredBy,
                ScoreCalculator.questionScore(weighted),
                results.stream()
                        .sorted(java.util.Comparator.comparing(r -> r.getCriterion().getCompetency().getCanonicalId()))
                        .map(CriterionScoreResponse::from)
                        .toList());
    }
}
