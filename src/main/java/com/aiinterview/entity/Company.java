package com.aiinterview.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ERD: COMPANY
 * status: "draft_library"(리서치·역량 매칭 진행 중) | "verified"(사람 교차검증 완료된 Verified 루브릭 보유)
 * 코레일 Phase 0는 이 status가 verified로 올라가는 첫 사례가 된다.
 */
@Getter
@Setter
@Entity
@Table(name = "company")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "official_domain")
    private String officialDomain;

    @Column(nullable = false)
    @Builder.Default
    private String status = "DRAFT_LIBRARY";

    /** 사용자가 "코레일", "한전", "금감원"처럼 줄여 부르는 이름으로도 찾을 수 있게 한다. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "company_alias", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "alias", nullable = false)
    @Builder.Default
    private java.util.Set<String> aliases = new java.util.LinkedHashSet<>();

    /** 인재상 원문을 확인해 실었는지. false면 공통(NCS) 기준으로만 평가한다. */
    @Column(name = "competency_confirmed", nullable = false)
    @Builder.Default
    private boolean competencyConfirmed = false;
}
