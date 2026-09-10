package com.aiinterview.dto.response;

import com.aiinterview.entity.EvaluationResult;

import java.util.UUID;

public record CriterionScoreResponse(
        UUID criterionId,
        String canonicalId,
        String competencyLabel,
        Integer weightPct,
        Integer aiScore,
        String evidenceText
) {
    public static CriterionScoreResponse from(EvaluationResult result) {
        return new CriterionScoreResponse(
                result.getCriterion().getId(),
                result.getCriterion().getCompetency().getCanonicalId(),
                result.getCriterion().getCompetency().getLabel(),
                result.getCriterion().getWeightPct(),
                result.getAiScore(),
                result.getEvidenceText());
    }
}
