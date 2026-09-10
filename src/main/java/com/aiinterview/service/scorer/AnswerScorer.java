package com.aiinterview.service.scorer;

import java.util.List;

/**
 * 채점기 포트. 구현체는 Claude 호출(ClaudeAnswerScorer) 또는 결정적 스텁(StubAnswerScorer).
 *
 * 계약: criterion마다 1~5점 정수와 근거를 하나씩 낸다. 총점은 내지 않는다 —
 * 총점은 EvaluationService가 Σ(score/5 × weightPct)로 계산한다(CLAUDE.md 원칙 1).
 */
public interface AnswerScorer {
    List<CriterionScore> score(ScoringRequest request);

    /** 결과 화면에 "무엇으로 채점했는지" 남기기 위한 이름. */
    String name();
}
