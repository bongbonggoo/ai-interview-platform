package com.aiinterview.dto.response;

public record CompetencyFeedbackResponse(
        String canonicalId,
        String companyLabel,      // 회사가 쓰는 표현 ("사람지향 소통인")
        String competencyLabel,   // 공통 역량명 ("의사소통능력")
        Integer score,            // 1~5
        String scoreCriterion,    // 그 점수에 해당하는 행동기준 문구
        String strengths,
        String gaps,
        String quotedEvidence
) {}
