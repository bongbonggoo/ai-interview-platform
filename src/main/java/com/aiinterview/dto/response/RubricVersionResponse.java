package com.aiinterview.dto.response;

import com.aiinterview.entity.RubricVersion;

import java.time.Instant;
import java.util.UUID;

public record RubricVersionResponse(
        UUID id,
        UUID companyId,
        Integer versionNumber,
        String status,
        String changelog,
        Instant createdAt
) {
    public static RubricVersionResponse from(RubricVersion rubricVersion) {
        return new RubricVersionResponse(
                rubricVersion.getId(),
                rubricVersion.getCompany().getId(),
                rubricVersion.getVersionNumber(),
                rubricVersion.getStatus(),
                rubricVersion.getChangelog(),
                rubricVersion.getCreatedAt()
        );
    }
}
