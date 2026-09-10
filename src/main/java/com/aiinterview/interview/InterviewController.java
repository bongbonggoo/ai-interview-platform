package com.aiinterview.interview;

import com.aiinterview.interview.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 면접 응시 흐름 전체의 창구.
 * 면접 시작 -> 질문(턴) 기록 -> 답변 제출 -> 종료 -> 조회.
 */
@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewSessionService interviewSessionService;
    private final InterviewTurnService interviewTurnService;
    private final InterviewAnswerService interviewAnswerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewSessionResponse create(@Valid @RequestBody InterviewSessionCreateRequest request) {
        return interviewSessionService.create(request);
    }

    @GetMapping("/{sessionId}")
    public InterviewSessionResponse getById(@PathVariable UUID sessionId) {
        return interviewSessionService.getById(sessionId);
    }

    /** 세션 + 턴 + 답변을 한 번에 — 결과 화면과 채점기가 쓰는 뷰. */
    @GetMapping("/{sessionId}/detail")
    public InterviewDetailResponse getDetail(@PathVariable UUID sessionId) {
        return interviewSessionService.getDetail(sessionId);
    }

    @GetMapping
    public List<InterviewSessionResponse> getByUser(@RequestParam String userIdentifier) {
        return interviewSessionService.getByUser(userIdentifier);
    }

    @PostMapping("/{sessionId}/end")
    public InterviewSessionResponse end(@PathVariable UUID sessionId) {
        return interviewSessionService.end(sessionId);
    }

    @PostMapping("/{sessionId}/turns")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewTurnResponse createTurn(@PathVariable UUID sessionId,
                                            @Valid @RequestBody InterviewTurnCreateRequest request) {
        return interviewTurnService.create(sessionId, request);
    }

    @GetMapping("/{sessionId}/turns")
    public List<InterviewTurnResponse> getTurns(@PathVariable UUID sessionId) {
        return interviewTurnService.getBySession(sessionId);
    }

    @PostMapping("/{sessionId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewAnswerResponse submitAnswer(@PathVariable UUID sessionId,
                                                @Valid @RequestBody InterviewAnswerCreateRequest request) {
        return interviewAnswerService.submit(sessionId, request);
    }

    @GetMapping("/{sessionId}/turns/{turnId}/answer")
    public InterviewAnswerResponse getAnswer(@PathVariable UUID sessionId, @PathVariable UUID turnId) {
        return interviewAnswerService.getByTurn(sessionId, turnId);
    }
}
