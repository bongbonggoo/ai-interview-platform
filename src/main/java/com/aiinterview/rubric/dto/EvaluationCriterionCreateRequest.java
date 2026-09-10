package com.aiinterview.rubric.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * anchors를 같이 받는 이유: 시드 삽입(코레일 04~07 시트)에서 criterion 하나와 1/3/5 앵커는
 * 사실상 한 덩어리라, 왕복 4번 대신 한 번에 넣을 수 있게 한다. 생략하면 나중에 따로 채워도 된다.
 */
public record EvaluationCriterionCreateRequest(
        @NotNull(message = "questionId는 필수입니다") UUID questionId,
        @NotBlank(message = "canonicalId(역량 식별자)는 필수입니다") String canonicalId,
        @NotNull(message = "weightPct는 필수입니다")
        @Min(value = 1, message = "weightPct는 1 이상이어야 합니다")
        @Max(value = 100, message = "weightPct는 100 이하여야 합니다")
        Integer weightPct,
        @Valid List<ScoreAnchorRequest> anchors
) {}
