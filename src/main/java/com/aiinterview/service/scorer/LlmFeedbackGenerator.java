package com.aiinterview.service.scorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

/**
 * 사람이 자소서를 들고 와서 "이거 어때요?"라고 물었을 때 하는 피드백을 그대로 재현한다.
 * 회사 인재상을 먼저 놓고, 글에서 그 역량의 근거를 찾아 짚어주고, 빠진 것을 말해준다.
 *
 * 절대 하지 않는 것: 모범답안 제시, 문장 고쳐쓰기, 총점 계산.
 * 프롬프트는 FeedbackPrompt에 있어 어떤 모델을 쓰든 성격이 갈리지 않는다.
 */
@RequiredArgsConstructor
public class LlmFeedbackGenerator implements FeedbackGenerator {

    private final LlmClient llm;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return llm.name();
    }

    @Override
    public FeedbackResult generate(FeedbackContext context) {
        return FeedbackPrompt.parse(objectMapper,
                llm.complete(FeedbackPrompt.system(), FeedbackPrompt.user(context)));
    }
}
