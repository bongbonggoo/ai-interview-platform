package com.aiinterview.rubric.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** level은 1/3/5만 허용(BARS 3점 앵커). 값 검증은 서비스에서 한 번 더 한다. */
public record ScoreAnchorRequest(
        @NotNull(message = "level은 필수입니다") Integer level,
        @NotBlank(message = "행동기준 설명은 필수입니다") String description
) {}
