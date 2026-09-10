package com.aiinterview.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 총점 계산은 순수 함수다 — AI가 아니라 이 코드가 총점을 만든다는 사실을 여기에 못박는다. */
class ScoreCalculatorTest {

    @Test
    @DisplayName("질문 점수 = Σ(ai_score / 5 × weight_pct)")
    void questionScore() {
        // 4점(60%) + 2점(40%) = 48 + 16
        assertThat(ScoreCalculator.questionScore(List.of(
                new ScoreCalculator.Weighted(4, 60),
                new ScoreCalculator.Weighted(2, 40)))).isEqualTo(64.0);
    }

    @Test
    @DisplayName("모두 5점이면 100점, 모두 1점이면 20점")
    void boundaries() {
        assertThat(ScoreCalculator.questionScore(List.of(
                new ScoreCalculator.Weighted(5, 70), new ScoreCalculator.Weighted(5, 30)))).isEqualTo(100.0);
        assertThat(ScoreCalculator.questionScore(List.of(
                new ScoreCalculator.Weighted(1, 70), new ScoreCalculator.Weighted(1, 30)))).isEqualTo(20.0);
    }

    @Test
    @DisplayName("세션 총점은 질문 점수들의 평균이고, 채점된 게 없으면 null")
    void sessionScore() {
        assertThat(ScoreCalculator.sessionScore(List.of(64.0, 80.0))).isEqualTo(72.0);
        assertThat(ScoreCalculator.sessionScore(List.of())).isNull();
    }
}
