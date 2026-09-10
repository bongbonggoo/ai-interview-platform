package com.aiinterview.service.scorer;

import java.util.List;

/** 질문 생성 입력. 기준이 되는 역량은 그 기관의 인재상에서만 온다. */
public record QuestionContext(
        String companyName,
        boolean competencyConfirmed,
        String jobTitle,          // 없을 수 있음
        int count,
        List<Competency> competencies
) {
    public record Competency(String canonicalId, String companyLabel, String label, String definition) {}
}
