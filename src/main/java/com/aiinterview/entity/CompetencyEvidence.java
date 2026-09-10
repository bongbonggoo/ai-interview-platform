package com.aiinterview.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ERD: COMPETENCY_EVIDENCE — "이 회사가 이 역량을 요구한다"는 근거.
 *
 * 근거 없는 역량은 쓰지 않는다는 원칙의 집행 지점이다. sourceUrl 없이는 만들 수 없고,
 * 화면에도 출처를 그대로 노출해서 사용자가 직접 확인할 수 있게 한다.
 * companyLabel은 회사가 쓰는 원문 표현("사람지향 소통인")이고,
 * competency는 그것을 매칭한 회사 무관 canonical 역량이다(원칙 2).
 */
@Getter
@Setter
@Entity
@Table(name = "competency_evidence")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetencyEvidence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canonical_id", nullable = false)
    private CompetencyLibrary competency;

    @Column(name = "company_label", nullable = false)
    private String companyLabel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "source_title")
    private String sourceTitle;
}
