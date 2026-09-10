package com.aiinterview.rubric.dto;

import com.aiinterview.rubric.ScoreAnchor;

import java.util.UUID;

public record ScoreAnchorResponse(
        UUID id,
        UUID criterionId,
        Integer level,
        String description
) {
    public static ScoreAnchorResponse from(ScoreAnchor anchor) {
        return new ScoreAnchorResponse(
                anchor.getId(),
                anchor.getCriterion().getId(),
                anchor.getLevel(),
                anchor.getDescription()
        );
    }
}
