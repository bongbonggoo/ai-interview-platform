package com.aiinterview.interview.dto;

import com.aiinterview.interview.InterviewTurn;

import java.time.Instant;
import java.util.UUID;

public record InterviewTurnResponse(
        UUID turnId,
        UUID sessionId,
        UUID questionId,
        String questionText,
        String turnType,
        Integer orderIndex,
        Instant createdAt
) {
    public static InterviewTurnResponse from(InterviewTurn turn) {
        return new InterviewTurnResponse(
                turn.getId(),
                turn.getSession().getId(),
                turn.getQuestion() == null ? null : turn.getQuestion().getId(),
                turn.getQuestionTextSnapshot(),
                turn.getTurnType(),
                turn.getOrderIndex(),
                turn.getCreatedAt());
    }
}
