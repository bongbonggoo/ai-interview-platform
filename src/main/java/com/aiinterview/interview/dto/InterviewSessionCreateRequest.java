package com.aiinterview.interview.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * companyId를 따로 받는 이유: jobPosition/rubricVersion이 정말 같은 회사 것인지
 * 서버가 교차 검증할 수 있게 하기 위함(다른 회사 루브릭으로 채점되는 사고 방지).
 */
public record InterviewSessionCreateRequest(
        String userIdentifier,
        @NotNull(message = "companyId는 필수입니다") UUID companyId,
        @NotNull(message = "jobPositionId는 필수입니다") UUID jobPositionId,
        @NotNull(message = "rubricVersionId는 필수입니다") UUID rubricVersionId,
        String interviewerStyle
) {}
