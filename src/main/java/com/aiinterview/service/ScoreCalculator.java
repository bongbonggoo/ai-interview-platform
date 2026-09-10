package com.aiinterview.service;

import java.util.List;

/**
 * 총점은 여기서만 계산된다. AI는 항목 점수만 내고, 총점을 내지도 저장하지도 않는다(CLAUDE.md 원칙 1).
 *
 * 한 질문의 점수 = Σ(ai_score / 5 × weight_pct). 가중치 합이 100이므로 0~100 범위가 된다
 * (합이 100인 것은 RubricVersionService.verify가 보장한다).
 */
public final class ScoreCalculator {

    private static final int MAX_SCORE = 5;

    private ScoreCalculator() {}

    public record Weighted(int aiScore, int weightPct) {}

    public static double questionScore(List<Weighted> scores) {
        double sum = scores.stream()
                .mapToDouble(w -> (double) w.aiScore() / MAX_SCORE * w.weightPct())
                .sum();
        return round1(sum);
    }

    /** 세션 총점 = 채점된 질문 점수들의 평균(질문 간 가중치는 아직 없다). */
    public static Double sessionScore(List<Double> questionScores) {
        if (questionScores.isEmpty()) return null;
        return round1(questionScores.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
