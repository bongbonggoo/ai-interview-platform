package com.aiinterview.service.scorer;

import java.util.List;

public interface QuestionGenerator {
    List<GeneratedQuestion> generate(QuestionContext context);
    String name();
}
