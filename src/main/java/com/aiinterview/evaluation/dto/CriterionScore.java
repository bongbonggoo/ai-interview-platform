package com.aiinterview.evaluation.dto;

import java.util.UUID;

/** 채점기가 criterion 하나에 대해 내놓는 것: 1~5점과 근거. 총점은 절대 여기 없다. */
public record CriterionScore(
        UUID criterionId,
        Integer score,
        String evidence
) {}
