package com.aiinterview.dto.response;

import java.util.List;

public record QuestionGenerateResponse(
        String companyName,
        boolean competencyConfirmed,
        String sourceUrl,
        String sourceTitle,
        String generatedBy,
        List<GeneratedQuestionResponse> questions
) {}
