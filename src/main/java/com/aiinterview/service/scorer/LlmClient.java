package com.aiinterview.service.scorer;

/**
 * 모델 호출 배관만 담당한다. 무엇을 시킬지(프롬프트)는 호출하는 쪽이 정한다.
 * 피드백과 질문 생성이 같은 배관을 쓰도록 분리해 둔 것 — 모델을 바꿔도 두 기능이 함께 따라간다.
 */
public interface LlmClient {
    String complete(String systemPrompt, String userPrompt);
    String name();
}
