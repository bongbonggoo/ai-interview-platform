package com.aiinterview.company.dto;

import com.aiinterview.company.Company;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String officialDomain,
        String status,
        Instant createdAt
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getOfficialDomain(),
                company.getStatus(),
                company.getCreatedAt()
        );
    }
}
