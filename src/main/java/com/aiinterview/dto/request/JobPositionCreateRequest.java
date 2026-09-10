package com.aiinterview.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JobPositionCreateRequest(
        @NotBlank(message = "직무명은 필수입니다") String name
) {}
