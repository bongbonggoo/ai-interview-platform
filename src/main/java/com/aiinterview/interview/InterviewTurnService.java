package com.aiinterview.interview;

import com.aiinterview.interview.dto.InterviewTurnCreateRequest;
import com.aiinterview.interview.dto.InterviewTurnResponse;
import com.aiinterview.question.Question;
import com.aiinterview.question.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * 턴 = "이 세션에서 실제로 물어본 한 번".
 *
 * questionTextSnapshot을 서버가 Question에서 복사하는 게 이 서비스의 요점이다.
 * 나중에 Question 원본 문구가 수정돼도 이 세션에서 실제로 나간 문구는 그대로 남아야
 * "왜 이 답이 이 점수인가"를 재현할 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewTurnService {

    public static final String TYPE_MAIN = "main";
    public static final String TYPE_FOLLOW_UP = "follow_up";

    private final InterviewTurnRepository interviewTurnRepository;
    private final InterviewSessionService interviewSessionService;
    private final QuestionService questionService;

    @Transactional
    public InterviewTurnResponse create(UUID sessionId, InterviewTurnCreateRequest request) {
        InterviewSession session = interviewSessionService.getOpenSession(sessionId);

        Question question = null;
        String snapshot;
        String turnType;

        if (request.questionId() != null) {
            question = questionService.getEntity(request.questionId());
            if (!question.getCompany().getId().equals(session.getCompany().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "질문이 이 세션의 회사 소속이 아닙니다 (질문 회사=" + question.getCompany().getId() + ")");
            }
            snapshot = question.getQuestionText();
            turnType = TYPE_MAIN;
        } else {
            // 즉석 꼬리질문 — 원본 Question이 없으므로 문구를 직접 받아야 한다.
            if (request.questionText() == null || request.questionText().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "questionId 없이 턴을 만들려면(꼬리질문) questionText가 필요합니다");
            }
            snapshot = request.questionText();
            turnType = TYPE_FOLLOW_UP;
        }

        int nextOrderIndex = interviewTurnRepository.findTopBySessionIdOrderByOrderIndexDesc(sessionId)
                .map(latest -> latest.getOrderIndex() + 1)
                .orElse(1);

        InterviewTurn turn = InterviewTurn.builder()
                .session(session)
                .question(question)
                .questionTextSnapshot(snapshot)
                .turnType(turnType)
                .orderIndex(nextOrderIndex)
                .build();

        return InterviewTurnResponse.from(interviewTurnRepository.saveAndFlush(turn));
    }

    public List<InterviewTurnResponse> getBySession(UUID sessionId) {
        interviewSessionService.getEntity(sessionId);
        return interviewTurnRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).stream()
                .map(InterviewTurnResponse::from)
                .toList();
    }

    /** 답변을 붙일 때, 그 턴이 정말 이 세션의 턴인지까지 확인한다. */
    InterviewTurn getEntityInSession(UUID sessionId, UUID turnId) {
        InterviewTurn turn = interviewTurnRepository.findById(turnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "턴을 찾을 수 없습니다: " + turnId));

        if (!turn.getSession().getId().equals(sessionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "턴 " + turnId + "은 세션 " + sessionId + "의 턴이 아닙니다");
        }
        return turn;
    }
}
