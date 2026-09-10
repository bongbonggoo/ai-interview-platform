package com.aiinterview.company;

import com.aiinterview.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
    private String status = "draft_library";
}
