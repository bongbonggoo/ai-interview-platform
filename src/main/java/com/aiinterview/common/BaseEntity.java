package com.aiinterview.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * 모든 엔티티가 공유하는 PK(UUID)와 생성시각.
 * ERD의 모든 테이블이 uuid id PK + created_at을 갖는 규칙을 여기 한 곳에 모아둔다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
