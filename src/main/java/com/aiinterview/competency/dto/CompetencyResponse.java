package com.aiinterview.competency.dto;

import com.aiinterview.competency.CompetencyLibrary;

public record CompetencyResponse(
        String canonicalId,
        String label,
        String definition,
        String tier
) {
    public static CompetencyResponse from(CompetencyLibrary competency) {
        return new CompetencyResponse(
                competency.getCanonicalId(),
                competency.getLabel(),
                competency.getDefinition(),
                competency.getTier()
        );
    }
}
