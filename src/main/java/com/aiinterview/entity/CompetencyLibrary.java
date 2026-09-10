package com.aiinterview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ERD: COMPETENCY_LIBRARY — 회사 무관, 재사용되는 canonical 역량 정의.
 * PK가 UUID가 아니라 canonical_id 문자열인 이유: 코드 어디서나("COMMUNICATION" 등)
 * 사람이 읽을 수 있는 안정적인 식별자로 참조하기 위함(19개 초기 시드, 코레일 Phase 0에서 확정).
 * CompetencyEvidence/CompetencyAlias 등 근거 체인 테이블은 2차에서 추가.
 */
@Getter
@Setter
@Entity
@Table(name = "competency_library")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetencyLibrary {

    @Id
    @Column(name = "canonical_id", length = 64)
    private String canonicalId;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String definition;

    @Column(length = 1)
    private String tier; // A / B / C

    /**
     * 기본 채점 기준(BARS). "이 역량이 어떻게 드러났는가"를 관찰하는 행동기준이며 모범답안이 아니다.
     * 예시 답변을 여기에 넣으면 채점이 '정답과의 유사도 맞추기'가 되어 이 서비스의 전제가 무너진다.
     */
    @Column(name = "anchor_1", columnDefinition = "TEXT")
    private String anchor1;

    @Column(name = "anchor_3", columnDefinition = "TEXT")
    private String anchor3;

    @Column(name = "anchor_5", columnDefinition = "TEXT")
    private String anchor5;
}
