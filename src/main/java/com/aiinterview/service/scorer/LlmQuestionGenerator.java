package com.aiinterview.service.scorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class LlmQuestionGenerator implements QuestionGenerator {

    private final LlmClient llm;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return llm.name();
    }

    @Override
    public List<GeneratedQuestion> generate(QuestionContext context) {
        return QuestionPrompt.parse(objectMapper,
                llm.complete(QuestionPrompt.system(), QuestionPrompt.user(context)));
    }
}
