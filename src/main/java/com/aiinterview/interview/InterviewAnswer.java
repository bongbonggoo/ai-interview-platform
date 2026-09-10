package com.aiinterview.interview;

import com.aiinterview.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** ERD: INTERVIEW_ANSWER — Turn : Answer = 1:1(현재 설계). */
@Getter
@Setter
@Entity
@Table(name = "interview_answer")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewAnswer extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turn_id", nullable = false, unique = true)
    private InterviewTurn turn;

    @Column(name = "answer_text", columnDefinition = "TEXT", nullable = false)
    private String answerText;

    @Column(name = "response_seconds")
    private Integer responseSeconds;

    @Column(name = "submitted_at")
    private Instant submittedAt;
}
