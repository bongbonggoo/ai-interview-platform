package com.aiinterview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 사용자가 하는 일은 이게 전부다 — 기관 고르고, 글 붙여넣기.
 * 루브릭을 미리 만들어둘 필요도, 세션을 열 필요도 없다.
 */
public record FeedbackRequest(
        @NotBlank(message = "기관명은 필수입니다") String companyName,
        @Pattern(regexp = "COVER_LETTER|INTERVIEW_ANSWER",
                 message = "documentType은 COVER_LETTER 또는 INTERVIEW_ANSWER 입니다")
        String documentType,
        String question,
        @NotBlank(message = "글 내용은 필수입니다")
        @Size(min = 50, message = "피드백을 하려면 최소 50자 이상이 필요합니다")
        String content
) {
    public String documentTypeOrDefault() {
        return documentType == null || documentType.isBlank() ? "COVER_LETTER" : documentType;
    }
}
