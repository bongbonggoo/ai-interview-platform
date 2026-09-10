package com.aiinterview.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 답변 힌트나 모범답안 필드는 없다. 어떤 역량을 보는 질문인지(targets)와
 * 무엇을 확인하려는지(intent)까지만 보여준다.
 */
public record GeneratedQuestionResponse(
        UUID questionId,
        String questionText,
        String category,
        String intent,
        List<CompetencyTarget> targets
) {
    public record CompetencyTarget(String canonicalId, String companyLabel) {}
}
