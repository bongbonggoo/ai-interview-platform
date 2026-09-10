package com.aiinterview.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ERD: EVALUATION_CRITERION — (rubric_version, question, competency, weight_pct)의 조합.
 * 한 질문에 딸린 criterion들의 weight_pct 합이 100인지는 서비스 계층에서 검증
 * (코레일 워크북의 SUMIF 가중치 체크가 여기서는 애플리케이션 로직/DB 제약으로 옮겨간다).
 */
@Getter
@Setter
@Entity
@Table(name = "evaluation_criterion")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationCriterion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_version_id", nullable = false)
    private RubricVersion rubricVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canonical_id", nullable = false)
    private CompetencyLibrary competency;

    @Column(name = "weight_pct", nullable = false)
    private Integer weightPct;
}
