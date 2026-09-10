package com.aiinterview.evaluation.dto;

import java.util.List;
import java.util.UUID;

/**
 * 채점기에 넘기는 입력. 이 세션에 고정된 루브릭 버전에서 뽑은 criterion만 들어간다 —
 * 다른 회사나 다른 버전의 기준이 새어들면 "확정된 루브릭으로만 채점한다"가 깨진다.
 */
public record ScoringRequest(
        String companyName,
        String jobPositionName,
        String questionText,
        String answerText,
        List<Criterion> criteria
) {
    public record Criterion(
            UUID criterionId,
            String canonicalId,
            String label,
            String definition,
            int weightPct,
            String anchorLevel1,
            String anchorLevel3,
            String anchorLevel5
    ) {}
}
