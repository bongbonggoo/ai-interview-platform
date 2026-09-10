package com.aiinterview.rubric;

import com.aiinterview.common.BaseEntity;
import com.aiinterview.company.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ERD: RUBRIC_VERSION — 회사별 v1, v2, v3 ... (코레일 Phase 0 v1->v2->v3가 첫 실사례).
 * InterviewSession이 이 id를 고정 참조해서, 나중에 버전이 올라가도 과거 세션은 불변으로 남는다.
 */
@Getter
@Setter
@Entity
@Table(name = "rubric_version")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubricVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(nullable = false)
    @Builder.Default
    private String status = "draft"; // draft / verified

    @Column(columnDefinition = "TEXT")
    private String changelog;
}
