package com.aiinterview.service.scorer;

import java.util.List;

public record FeedbackResult(
        String overall,   // 전체 총평
        List<CompetencyVerdict> verdicts
) {}
