package com.aiinterview.service;

import com.aiinterview.dto.request.InterviewAnswerCreateRequest;
import com.aiinterview.dto.response.InterviewAnswerResponse;
import com.aiinterview.entity.InterviewAnswer;
import com.aiinterview.entity.InterviewTurn;
import com.aiinterview.repository.InterviewAnswerRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Turn : Answer = 1:1 (turn_id에 unique 제약). 그래서 재제출은 덮어쓰기가 아니라 409다 —
 * 답변을 나중에 갈아끼울 수 있으면 이미 나온 채점 결과가 어느 답변에 대한 것인지 알 수 없게 된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewAnswerService {

    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewSessionService interviewSessionService;
    private final InterviewTurnService interviewTurnService;

    @Transactional
    public InterviewAnswerResponse submit(UUID sessionId, InterviewAnswerCreateRequest request) {
        interviewSessionService.getOpenSession(sessionId);
        InterviewTurn turn = interviewTurnService.getEntityInSession(sessionId, request.turnId());

        if (interviewAnswerRepository.existsByTurnId(turn.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이 턴에는 이미 답변이 제출됐습니다: " + turn.getId());
        }

        InterviewAnswer answer = InterviewAnswer.builder()
                .turn(turn)
                .answerText(request.answerText())
                .responseSeconds(request.responseSeconds())
                .submittedAt(Instant.now())
                .build();

        return InterviewAnswerResponse.from(interviewAnswerRepository.saveAndFlush(answer));
    }

    public InterviewAnswerResponse getByTurn(UUID sessionId, UUID turnId) {
        InterviewTurn turn = interviewTurnService.getEntityInSession(sessionId, turnId);
        return interviewAnswerRepository.findByTurnId(turn.getId())
                .map(InterviewAnswerResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "아직 답변이 없습니다: 턴 " + turnId));
    }
}
