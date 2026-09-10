package com.aiinterview.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record InterviewAnswerCreateRequest(
        @NotNull(message = "turnId는 필수입니다") UUID turnId,
        @NotBlank(message = "답변 내용은 필수입니다") String answerText,
        @PositiveOrZero(message = "responseSeconds는 0 이상이어야 합니다") Integer responseSeconds
) {}
