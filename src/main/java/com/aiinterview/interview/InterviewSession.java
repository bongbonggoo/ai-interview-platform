package com.aiinterview.interview;

import com.aiinterview.common.BaseEntity;
import com.aiinterview.company.Company;
import com.aiinterview.company.JobPosition;
import com.aiinterview.rubric.RubricVersion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * ERD: INTERVIEW_SESSION
 * rubricVersion을 세션 시작 시점에 고정 저장 — 이후 그 회사 루브릭이 올라가도
 * 이미 끝난 세션의 채점 근거는 그대로 이 버전을 가리켜 재현 가능해야 한다.
 *
 * User/인증은 이번 1차 범위에서 의도적으로 제외 — 지금은 userIdentifier(문자열)로만
 * 누가 응시했는지 구분하고, 실제 회원 시스템은 별도 스프린트에서 붙인다.
 */
@Getter
@Setter
@Entity
@Table(name = "interview_session")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession extends BaseEntity {

    @Column(name = "user_identifier")
    private String userIdentifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_position_id", nullable = false)
    private JobPosition jobPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_version_id", nullable = false)
    private RubricVersion rubricVersion;

    @Column(name = "interviewer_style")
    private String interviewerStyle;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;
}
