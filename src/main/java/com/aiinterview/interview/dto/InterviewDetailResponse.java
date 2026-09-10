package com.aiinterview.interview.dto;

import java.util.List;

/**
 * 채점기가 필요로 하는 "어떤 회사 / 어떤 루브릭 버전 / 어떤 질문 / 뭐라고 답했나"를
 * 한 번에 돌려주는 뷰. 결과 화면과 EvaluationService가 같이 쓴다.
 */
public record InterviewDetailResponse(
        InterviewSessionResponse session,
        List<TurnWithAnswer> turns
) {
    public record TurnWithAnswer(
            InterviewTurnResponse turn,
            InterviewAnswerResponse answer // 미답변이면 null
    ) {}
}
