package com.aiinterview.controller;

import com.aiinterview.dto.response.AnswerEvaluationResponse;
import com.aiinterview.dto.response.InterviewResultResponse;
import com.aiinterview.service.EvaluationService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interviews/{sessionId}")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    /** 턴 하나 채점. */
    @PostMapping("/turns/{turnId}/evaluation")
    public AnswerEvaluationResponse evaluateTurn(@PathVariable UUID sessionId, @PathVariable UUID turnId) {
        return evaluationService.evaluateTurn(sessionId, turnId);
    }

    /** 아직 채점 안 된 답변 전부 채점. */
    @PostMapping("/evaluate")
    public List<AnswerEvaluationResponse> evaluateSession(@PathVariable UUID sessionId) {
        return evaluationService.evaluateSession(sessionId);
    }

    /** 결과 화면 — 항목 점수 + 근거 + 계산된 총점. */
    @GetMapping("/result")
    public InterviewResultResponse getResult(@PathVariable UUID sessionId) {
        return evaluationService.getResult(sessionId);
    }
}
