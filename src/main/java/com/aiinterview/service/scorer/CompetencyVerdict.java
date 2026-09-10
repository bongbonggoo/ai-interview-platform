package com.aiinterview.service.scorer;

/**
 * 역량 하나에 대한 판정. 점수와 "어디서 그렇게 봤는지"가 항상 같이 온다.
 * rewrite(고쳐 쓴 문장)나 modelAnswer 같은 필드는 의도적으로 없다.
 */
public record CompetencyVerdict(
        String canonicalId,
        Integer score,        // 1~5
        String strengths,     // 이 역량이 드러난 부분과 그 근거
        String gaps,          // 근거가 약한 부분과 무엇이 빠졌는지
        String quotedEvidence // 그렇게 판단한 원문 발췌
) {}
