package com.aiinterview.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ERD: QUESTION — 지금은 Company 종속(Company -> Question)으로 단순하게 간다.
 * 나중에 여러 회사에 재사용 가능한 질문이 늘어나면 QuestionTemplate을 분리하되,
 * 지금 단계에서 미리 일반화하지 않는다(설계 회의에서 합의된 방향).
 */
@Getter
@Setter
@Entity
@Table(name = "question")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    private String category;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "is_reusable_template", nullable = false)
    @Builder.Default
    private boolean reusableTemplate = false;
}
