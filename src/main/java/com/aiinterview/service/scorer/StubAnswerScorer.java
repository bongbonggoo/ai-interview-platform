package com.aiinterview.service.scorer;

import java.util.List;

/**
 * API 키 없이도 채점 파이프라인 전체(저장 -> 총점 계산 -> 결과 조회)를 돌려보기 위한 대역.
 *
 * 답변 길이로 3/4점을 나눌 뿐 내용을 이해하지 않는다. 절대 실제 채점에 쓰지 말 것 —
 * 결과 응답의 scoredBy 필드로 무엇이 채점했는지 항상 드러난다.
 */
public class StubAnswerScorer implements AnswerScorer {

    private static final int SUBSTANTIAL_ANSWER_LENGTH = 80;

    @Override
    public List<CriterionScore> score(ScoringRequest request) {
        int score = request.answerText().trim().length() >= SUBSTANTIAL_ANSWER_LENGTH ? 4 : 3;
        return request.criteria().stream()
                .map(criterion -> new CriterionScore(criterion.criterionId(), score,
                        "[스텁 채점기] 내용을 판단하지 않고 답변 길이로만 매긴 점수입니다. "
                                + "실제 채점은 ANTHROPIC_API_KEY를 설정해 Claude 채점기를 켜세요."))
                .toList();
    }

    @Override
    public String name() {
        return "stub";
    }
}
