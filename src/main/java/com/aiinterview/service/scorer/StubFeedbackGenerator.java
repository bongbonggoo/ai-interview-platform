package com.aiinterview.service.scorer;

import java.util.List;

/**
 * API 키 없이 화면·API를 확인하기 위한 대역. 글을 읽지 않고 길이만 본다.
 * 실제 피드백이 아니라는 사실이 응답의 scoredBy와 문구에 그대로 드러난다.
 */
public class StubFeedbackGenerator implements FeedbackGenerator {

    private static final int SUBSTANTIAL_LENGTH = 300;

    @Override
    public FeedbackResult generate(FeedbackContext context) {
        int score = context.content().trim().length() >= SUBSTANTIAL_LENGTH ? 3 : 2;
        List<CompetencyVerdict> verdicts = context.criteria().stream()
                .map(c -> new CompetencyVerdict(c.canonicalId(), score,
                        "[스텁] 글의 내용을 읽지 않았습니다.",
                        "[스텁] 실제 피드백을 받으려면 ANTHROPIC_API_KEY를 설정하세요.",
                        ""))
                .toList();
        return new FeedbackResult(
                "[스텁 생성기] 내용을 판단하지 않고 길이만 본 결과입니다. 실제 평가가 아닙니다.", verdicts);
    }

    @Override
    public String name() {
        return "stub";
    }
}
