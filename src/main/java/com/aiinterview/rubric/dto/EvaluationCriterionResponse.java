package com.aiinterview.rubric.dto;

import com.aiinterview.rubric.EvaluationCriterion;
import com.aiinterview.rubric.ScoreAnchor;

import java.util.List;
import java.util.UUID;

public record EvaluationCriterionResponse(
        UUID id,
        UUID rubricVersionId,
        UUID questionId,
        String canonicalId,
        String competencyLabel,
        Integer weightPct,
        List<ScoreAnchorResponse> anchors
) {
    public static EvaluationCriterionResponse from(EvaluationCriterion criterion, List<ScoreAnchor> anchors) {
        return new EvaluationCriterionResponse(
                criterion.getId(),
                criterion.getRubricVersion().getId(),
                criterion.getQuestion().getId(),
                criterion.getCompetency().getCanonicalId(),
                criterion.getCompetency().getLabel(),
                criterion.getWeightPct(),
                anchors.stream().map(ScoreAnchorResponse::from).toList()
        );
    }
}
