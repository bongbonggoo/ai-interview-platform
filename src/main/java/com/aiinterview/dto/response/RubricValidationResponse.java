package com.aiinterview.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * verified로 올리기 전에 사람이 확인할 수 있게, 무엇이 왜 막혔는지 목록으로 돌려준다.
 * (코레일 워크북의 SUMIF 가중치 체크가 여기로 옮겨온 자리다.)
 */
public record RubricValidationResponse(
        UUID rubricVersionId,
        boolean valid,
        List<String> problems
) {}
