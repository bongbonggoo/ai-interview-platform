package com.aiinterview.dto.request;

import java.util.UUID;

/**
 * questionId가 있으면 questionTextSnapshot은 서버가 Question에서 복사한다
 * (클라이언트가 준 문구를 믿으면 원본과 어긋난 스냅샷이 남는다).
 * questionId가 없으면 즉석 꼬리질문으로 보고 questionText를 필수로 받는다.
 * orderIndex는 세션별 max+1로 서버가 매긴다.
 */
public record InterviewTurnCreateRequest(
        UUID questionId,
        String questionText
) {}
