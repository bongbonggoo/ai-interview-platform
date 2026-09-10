package com.aiinterview.evaluation.dto;

import java.util.List;
import java.util.UUID;

/**
 * questionScore는 저장된 값이 아니라 조회할 때마다 계산한다 —
 * EvaluationResult에 총점 컬럼을 두지 않는다는 원칙을 유지하기 위함.
 */
public record AnswerEvaluationResponse(
        UUID answerId,
        UUID turnId,
        String questionText,
        String scoredBy,
        Double questionScore,
        List<CriterionScoreResponse> criterionScores
) {}
