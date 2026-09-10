package com.aiinterview.company.dto;

import jakarta.validation.constraints.NotBlank;

public record JobPositionCreateRequest(
        @NotBlank(message = "직무명은 필수입니다") String name
) {}
