package com.aiinterview.service.scorer;

/**
 * 피드백 생성기 포트. 채점기(AnswerScorer)와 같은 이유로 인터페이스로 분리한다 —
 * API 키 없이도 전체 흐름이 돌아야 하고, 계약 위반은 서비스 계층이 검증한다.
 */
public interface FeedbackGenerator {
    FeedbackResult generate(FeedbackContext context);
    String name();
}
