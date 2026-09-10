package com.aiinterview.company;

import com.aiinterview.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ERD: JOB_POSITION — Company 1:N (예: 코레일 → 사무영업, 토목 …) */
@Getter
@Setter
@Entity
@Table(name = "job_position")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPosition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String name;
}
