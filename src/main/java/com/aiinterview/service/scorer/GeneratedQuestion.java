package com.aiinterview.service.scorer;

import java.util.List;

/**
 * 생성된 면접 질문. 어떤 역량을 보려는 질문인지(targetCanonicalIds)와 왜 묻는지(intent)가 함께 온다.
 * 모범답안이나 답변 힌트 필드는 의도적으로 없다 — 있으면 사용자가 그걸 베끼게 된다.
 */
public record GeneratedQuestion(
        String questionText,
        String category,
        List<String> targetCanonicalIds,
        String intent
) {}
