package com.aiinterview.dto.response;

import com.aiinterview.entity.Company;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        List<String> aliases,
        String officialDomain,
        String status,
        boolean competencyConfirmed,
        Instant createdAt
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                List.copyOf(company.getAliases()),
                company.getOfficialDomain(),
                company.getStatus(),
                company.isCompetencyConfirmed(),
                company.getCreatedAt());
    }
}
