package com.aiinterview.rubric;

import com.aiinterview.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ERD: SCORE_ANCHOR — 한 Criterion의 1/3/5점 행동기준(BARS). */
@Getter
@Setter
@Entity
@Table(name = "score_anchor")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreAnchor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    private EvaluationCriterion criterion;

    @Column(nullable = false)
    private Integer level; // 1, 3, 5

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
}
