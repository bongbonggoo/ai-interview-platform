package com.aiinterview.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record QuestionGenerateRequest(
        @NotBlank(message = "기관명은 필수입니다") String companyName,
        String jobTitle,
        @Min(value = 1, message = "1개 이상") @Max(value = 10, message = "한 번에 10개까지") Integer count
) {
    public int countOrDefault() {
        return count == null ? 5 : count;
    }
}
