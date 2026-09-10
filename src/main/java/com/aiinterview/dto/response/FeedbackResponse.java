package com.aiinterview.dto.response;

import java.util.List;

/**
 * totalScore는 저장하지 않고 매번 계산한다. AI는 역량별 점수만 낸다(원칙 1).
 * sourceUrl을 같이 돌려주는 이유: 사용자가 "이 인재상 어디서 나온 거냐"를 직접 확인할 수 있어야 한다.
 */
public record FeedbackResponse(
        String companyName,
        boolean competencyConfirmed,
        String sourceUrl,
        String sourceTitle,
        String documentType,
        String scoredBy,
        Double totalScore,
        String overall,
        List<CompetencyFeedbackResponse> competencies
) {}
