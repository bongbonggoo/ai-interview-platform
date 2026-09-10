package com.aiinterview.company.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyCreateRequest(
        @NotBlank(message = "회사명은 필수입니다") String name,
        String officialDomain
) {}
