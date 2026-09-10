package com.aiinterview.question.dto;

import jakarta.validation.constraints.NotBlank;

public record QuestionCreateRequest(
        String category,
        @NotBlank(message = "질문 내용은 필수입니다") String questionText,
        Boolean reusableTemplate
) {}
