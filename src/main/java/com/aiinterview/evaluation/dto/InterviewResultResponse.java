package com.aiinterview.evaluation.dto;

import com.aiinterview.interview.dto.InterviewSessionResponse;

import java.util.List;

public record InterviewResultResponse(
        InterviewSessionResponse session,
        Double totalScore,
        int scoredAnswerCount,
        int unscoredAnswerCount,
        List<AnswerEvaluationResponse> answers
) {}
