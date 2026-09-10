package com.aiinterview.interview.dto;

import com.aiinterview.interview.InterviewSession;

import java.time.Instant;
import java.util.UUID;

/**
 * status는 컬럼이 아니라 endedAt에서 파생된다(ERD에 status 컬럼이 없다).
 * 상태를 늘려야 할 일이 생기면 그때 컬럼을 추가할 것 — 지금은 두 상태면 충분하다.
 */
public record InterviewSessionResponse(
        UUID sessionId,
        String userIdentifier,
        UUID companyId,
        UUID jobPositionId,
        UUID rubricVersionId,
        Integer rubricVersionNumber,
        String interviewerStyle,
        String status,
        Instant startedAt,
        Instant endedAt
) {
    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_COMPLETED = "completed";

    public static String statusOf(InterviewSession session) {
        return session.getEndedAt() == null ? STATUS_IN_PROGRESS : STATUS_COMPLETED;
    }

    public static InterviewSessionResponse from(InterviewSession session) {
        return new InterviewSessionResponse(
                session.getId(),
                session.getUserIdentifier(),
                session.getCompany().getId(),
                session.getJobPosition().getId(),
                session.getRubricVersion().getId(),
                session.getRubricVersion().getVersionNumber(),
                session.getInterviewerStyle(),
                statusOf(session),
                session.getStartedAt(),
                session.getEndedAt());
    }
}
