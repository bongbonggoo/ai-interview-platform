package com.aiinterview.service.scorer;

import java.util.List;

/**
 * 키 없이 화면·흐름을 확인하기 위한 대역.
 * 역량 이름을 끼워 넣은 틀 문장일 뿐 실제로 기관 맥락을 반영하지 않는다.
 */
public class StubQuestionGenerator implements QuestionGenerator {

    @Override
    public List<GeneratedQuestion> generate(QuestionContext context) {
        return context.competencies().stream()
                .limit(context.count())
                .map(c -> new GeneratedQuestion(
                        "[스텁 질문] " + c.companyLabel() + "이(가) 드러난 경험을 말씀해 주십시오.",
                        "직무",
                        List.of(c.canonicalId()),
                        "[스텁] 실제 질문은 API 키를 넣으면 생성됩니다."))
                .toList();
    }

    @Override
    public String name() {
        return "stub";
    }
}
