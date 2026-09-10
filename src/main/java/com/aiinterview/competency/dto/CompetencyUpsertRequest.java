package com.aiinterview.competency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompetencyUpsertRequest(
        @NotBlank(message = "역량 라벨은 필수입니다") String label,
        String definition,
        @Pattern(regexp = "[ABC]", message = "tier는 A/B/C 중 하나여야 합니다") String tier
) {}
