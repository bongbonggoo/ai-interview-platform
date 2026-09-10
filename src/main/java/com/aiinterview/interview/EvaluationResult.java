package com.aiinterview.interview;

import com.aiinterview.common.BaseEntity;
import com.aiinterview.rubric.EvaluationCriterion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ERD: EVALUATION_RESULT — Answer -> EvaluationResult -> Criterion -> Competency 체인.
 * "이 답변에서 왜 문제해결력이 4점인가?"를 aiScore + evidenceText로 추적 가능.
 * 총점은 여기 저장하지 않는다 — 서비스 계층에서 Σ(ai_score/5 × weight_pct)로 계산
 * (AI는 항목 점수만, 총점은 코드가 계산한다는 원칙을 그대로 유지).
 */
@Getter
@Setter
@Entity
@Table(name = "evaluation_result")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private InterviewAnswer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    private EvaluationCriterion criterion;

    @Column(name = "ai_score", nullable = false)
    private Integer aiScore; // 1-5, AI가 냄

    @Column(name = "evidence_text", columnDefinition = "TEXT")
    private String evidenceText; // AI가 낸 근거
}
