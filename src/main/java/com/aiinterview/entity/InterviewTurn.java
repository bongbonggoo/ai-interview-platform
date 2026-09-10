package com.aiinterview.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ERD: INTERVIEW_TURN — question은 nullable(꼬리질문은 즉석 생성이라 원본 Question이 없을 수 있음).
 * questionTextSnapshot으로 실제 그 순간 물어본 문구를 그대로 저장 — Question 원본이 나중에
 * 수정돼도 이 세션에서 실제로 나간 문구는 불변으로 남는다.
 */
@Getter
@Setter
@Entity
@Table(name = "interview_turn")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewTurn extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question; // nullable: 꼬리질문은 즉석 생성

    @Column(name = "question_text_snapshot", columnDefinition = "TEXT", nullable = false)
    private String questionTextSnapshot;

    @Column(name = "turn_type", nullable = false)
    private String turnType; // main / follow_up

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
