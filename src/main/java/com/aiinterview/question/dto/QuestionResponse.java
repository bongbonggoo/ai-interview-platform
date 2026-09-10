package com.aiinterview.question.dto;

import com.aiinterview.question.Question;

import java.time.Instant;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        UUID companyId,
        String category,
        String questionText,
        boolean reusableTemplate,
        Instant createdAt
) {
    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getCompany().getId(),
                question.getCategory(),
                question.getQuestionText(),
                question.isReusableTemplate(),
                question.getCreatedAt()
        );
    }
}
