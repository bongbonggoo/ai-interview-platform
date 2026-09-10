package com.aiinterview.config;

import com.aiinterview.service.scorer.AnswerScorer;
import com.aiinterview.service.scorer.ClaudeAnswerScorer;
import com.aiinterview.service.scorer.ClaudeFeedbackGenerator;
import com.aiinterview.service.scorer.FeedbackGenerator;
import com.aiinterview.service.scorer.StubAnswerScorer;
import com.aiinterview.service.scorer.StubFeedbackGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 키가 있으면 Claude 채점기, 없으면 스텁.
 * 키가 없다고 앱이 안 뜨면 로컬에서 면접 흐름 자체를 못 돌려보게 되므로 이렇게 갈라놓는다.
 */
@Configuration
@EnableConfigurationProperties(ClaudeProperties.class)
public class ScoringConfig {

    private static final Logger log = LoggerFactory.getLogger(ScoringConfig.class);

    @Bean
    public AnswerScorer answerScorer(ClaudeProperties properties, ObjectMapper objectMapper) {
        if (!properties.hasApiKey()) {
            log.warn("ANTHROPIC_API_KEY가 없어 스텁 채점기로 뜹니다. 채점 결과는 실제 평가가 아닙니다.");
            return new StubAnswerScorer();
        }
        log.info("Claude 채점기 활성화 (model={})", properties.model());
        return new ClaudeAnswerScorer(RestClient.create(), objectMapper, properties);
    }

    @Bean
    public FeedbackGenerator feedbackGenerator(ClaudeProperties properties, ObjectMapper objectMapper) {
        if (!properties.hasApiKey()) {
            log.warn("ANTHROPIC_API_KEY가 없어 스텁 피드백 생성기로 뜹니다. 실제 피드백이 아닙니다.");
            return new StubFeedbackGenerator();
        }
        return new ClaudeFeedbackGenerator(RestClient.create(), objectMapper, properties);
    }
}
