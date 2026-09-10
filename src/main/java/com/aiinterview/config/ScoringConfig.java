package com.aiinterview.config;

import com.aiinterview.service.scorer.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 있는 키를 쓴다: Claude -> Gemini -> 스텁.
 * 키가 없다고 앱이 안 뜨면 화면과 흐름조차 못 보게 되므로 스텁으로 떨어뜨린다.
 */
@Configuration
@EnableConfigurationProperties({ClaudeProperties.class, GeminiProperties.class})
public class ScoringConfig {

    private static final Logger log = LoggerFactory.getLogger(ScoringConfig.class);

    // 키가 없으면 LlmClient 자체가 없다. 빈으로 등록하면 null 주입이 되지 않으므로
    // 여기서 한 번만 만들어 두고 생성기들이 나눠 쓴다.
    private LlmClient resolved;
    private boolean resolvedOnce;

    private synchronized LlmClient llmClient(ClaudeProperties claude, GeminiProperties gemini) {
        if (resolvedOnce) return resolved;
        resolvedOnce = true;
        resolved = createLlmClient(claude, gemini);
        return resolved;
    }

    private LlmClient createLlmClient(ClaudeProperties claude, GeminiProperties gemini) {
        if (claude.hasApiKey()) {
            warnIfOddFormat("ANTHROPIC_API_KEY", claude.apiKey(), "sk-ant-",
                    "https://console.anthropic.com");
            log.info("Claude 사용 (model={})", claude.model());
            return new ClaudeLlmClient(RestClient.create(), claude);
        }
        if (gemini.hasApiKey()) {
            warnIfOddFormat("GEMINI_API_KEY", gemini.apiKey(), "AQ.",
                    "https://aistudio.google.com/app/apikey");
            log.info("Gemini 사용 (model={})", gemini.model());
            return new GeminiLlmClient(RestClient.create(), gemini);
        }
        log.warn("API 키가 없어 스텁으로 뜹니다. 실제 피드백·질문이 아닙니다. "
                + "ANTHROPIC_API_KEY 또는 GEMINI_API_KEY를 설정하세요.");
        return null;
    }

    /**
     * 401이 났을 때 키를 통째로 다시 붙여넣게 하지 않으려면, 앱이 실제로 무엇을 받았는지 보여줘야 한다.
     * 키 값은 절대 로그에 남기지 않고 앞 몇 글자와 길이만 남긴다.
     */
    private static void warnIfOddFormat(String envName, String key, String expectedPrefix, String issueUrl) {
        String masked = key.length() <= 8 ? key.charAt(0) + "…" : key.substring(0, 8) + "…";
        log.info("{} 인식됨: {} (길이 {}자)", envName, masked, key.length());
        if (!key.startsWith(expectedPrefix)) {
            log.warn("{}가 '{}'로 시작하지 않습니다. 값을 잘못 붙여넣었을 수 있습니다 ({}에서 확인).",
                    envName, expectedPrefix, issueUrl);
        }
        if (!key.equals(key.trim()) || key.contains("\"") || key.contains("'")) {
            log.warn("{}에 따옴표나 공백이 섞여 있습니다. 따옴표 없이, 앞뒤 공백 없이 넣어야 합니다.", envName);
        }
    }

    @Bean
    public FeedbackGenerator feedbackGenerator(ClaudeProperties claude, GeminiProperties gemini,
                                               ObjectMapper objectMapper) {
        LlmClient llm = llmClient(claude, gemini);
        return llm == null ? new StubFeedbackGenerator() : new LlmFeedbackGenerator(llm, objectMapper);
    }

    @Bean
    public QuestionGenerator questionGenerator(ClaudeProperties claude, GeminiProperties gemini,
                                               ObjectMapper objectMapper) {
        LlmClient llm = llmClient(claude, gemini);
        return llm == null ? new StubQuestionGenerator() : new LlmQuestionGenerator(llm, objectMapper);
    }

    /** 면접 답변 채점기는 아직 Claude 전용 경로를 쓴다(질문/피드백과 프롬프트 성격이 다르다). */
    @Bean
    public AnswerScorer answerScorer(ClaudeProperties properties, ObjectMapper objectMapper) {
        if (!properties.hasApiKey()) {
            return new StubAnswerScorer();
        }
        return new ClaudeAnswerScorer(RestClient.create(), objectMapper, properties);
    }
}
