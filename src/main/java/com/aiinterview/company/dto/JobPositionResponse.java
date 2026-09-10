package com.aiinterview.company.dto;

import com.aiinterview.company.JobPosition;

import java.time.Instant;
import java.util.UUID;

public record JobPositionResponse(
        UUID id,
        UUID companyId,
        String name,
        Instant createdAt
) {
    public static JobPositionResponse from(JobPosition jobPosition) {
        return new JobPositionResponse(
                jobPosition.getId(),
                jobPosition.getCompany().getId(),
                jobPosition.getName(),
                jobPosition.getCreatedAt());
    }
}
