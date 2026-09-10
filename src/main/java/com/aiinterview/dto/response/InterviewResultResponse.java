package com.aiinterview.dto.response;

import java.util.List;

public record InterviewResultResponse(
        InterviewSessionResponse session,
        Double totalScore,
        int scoredAnswerCount,
        int unscoredAnswerCount,
        List<AnswerEvaluationResponse> answers
) {}
