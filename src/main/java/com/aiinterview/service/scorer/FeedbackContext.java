package com.aiinterview.service.scorer;

import java.util.List;

/**
 * 피드백 생성기에 넘기는 입력.
 * criteria에는 그 회사의 인재상(원문 표현)과 canonical 역량의 1/3/5 행동기준이 함께 들어간다.
 * 모범답안은 여기에도, 프롬프트에도 없다 — 있을 수 없다.
 */
public record FeedbackContext(
        String companyName,
        boolean competencyConfirmed,
        String documentType,   // COVER_LETTER / INTERVIEW_ANSWER
        String question,       // 자소서 문항 또는 면접 질문 (없을 수 있음)
        String content,
        List<Criterion> criteria
) {
    public record Criterion(
            String canonicalId,
            String companyLabel,     // 회사가 쓰는 표현. 미확인 기관이면 canonical 라벨과 같다.
            String competencyLabel,  // canonical 역량명
            String definition,
            String anchor1,
            String anchor3,
            String anchor5
    ) {}
}
