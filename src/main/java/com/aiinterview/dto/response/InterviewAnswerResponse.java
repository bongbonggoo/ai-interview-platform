package com.aiinterview.dto.response;

import com.aiinterview.entity.InterviewAnswer;

import java.time.Instant;
import java.util.UUID;

public record InterviewAnswerResponse(
        UUID answerId,
        UUID turnId,
        String answerText,
        Integer responseSeconds,
        Instant submittedAt
) {
    public static InterviewAnswerResponse from(InterviewAnswer answer) {
        return new InterviewAnswerResponse(
                answer.getId(),
                answer.getTurn().getId(),
                answer.getAnswerText(),
                answer.getResponseSeconds(),
                answer.getSubmittedAt());
    }
}
