package com.aiinterview.service;

import com.aiinterview.dto.request.InterviewSessionCreateRequest;
import com.aiinterview.dto.response.InterviewAnswerResponse;
import com.aiinterview.dto.response.InterviewDetailResponse;
import com.aiinterview.dto.response.InterviewSessionResponse;
import com.aiinterview.dto.response.InterviewTurnResponse;
import com.aiinterview.entity.Company;
import com.aiinterview.entity.InterviewAnswer;
import com.aiinterview.entity.InterviewSession;
import com.aiinterview.entity.JobPosition;
import com.aiinterview.entity.RubricVersion;
import com.aiinterview.repository.CompanyRepository;
import com.aiinterview.repository.InterviewAnswerRepository;
import com.aiinterview.repository.InterviewSessionRepository;
import com.aiinterview.repository.InterviewTurnRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 면접 세션의 수명주기.
 *
 * 세션을 만들 때 rubricVersion을 고정 저장하는 것이 이 서비스의 핵심이다(CLAUDE.md 원칙 3).
 * 이후 회사 루브릭이 v4로 올라가도 이 세션의 채점 근거는 시작 시점 버전을 계속 가리킨다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewSessionService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewTurnRepository interviewTurnRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final CompanyRepository companyRepository;
    private final JobPositionService jobPositionService;
    private final RubricVersionService rubricVersionService;

    @Transactional
    public InterviewSessionResponse create(InterviewSessionCreateRequest request) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "회사를 찾을 수 없습니다: " + request.companyId()));

        JobPosition jobPosition = jobPositionService.getEntity(request.jobPositionId());
        if (!jobPosition.getCompany().getId().equals(company.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "직무가 이 회사 소속이 아닙니다 (직무 회사=" + jobPosition.getCompany().getId() + ")");
        }

        RubricVersion rubricVersion = rubricVersionService.getEntity(request.rubricVersionId());
        if (!rubricVersion.getCompany().getId().equals(company.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "루브릭이 이 회사 소속이 아닙니다 (루브릭 회사=" + rubricVersion.getCompany().getId() + ")");
        }

        // 확정되지 않은 루브릭으로 면접을 시작하면, 나중에 그 루브릭이 바뀌어도 세션은 옛 draft를
        // 계속 가리키게 된다. "회사별로 확정된 루브릭으로만 채점한다"는 전제가 여기서 깨지므로 막는다.
        if (!RubricVersionService.STATUS_VERIFIED.equals(rubricVersion.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "아직 verified되지 않은 루브릭 v" + rubricVersion.getVersionNumber() + "으로는 면접을 시작할 수 없습니다. "
                            + "POST /rubric-versions/" + rubricVersion.getId() + "/verify 로 먼저 확정하세요");
        }

        InterviewSession session = InterviewSession.builder()
                .userIdentifier(request.userIdentifier())
                .company(company)
                .jobPosition(jobPosition)
                .rubricVersion(rubricVersion)
                .interviewerStyle(request.interviewerStyle())
                .startedAt(Instant.now())
                .build();

        return InterviewSessionResponse.from(interviewSessionRepository.saveAndFlush(session));
    }

    /** 이미 끝난 세션이면 그대로 돌려준다(같은 요청을 두 번 보내도 안전하게). */
    @Transactional
    public InterviewSessionResponse end(UUID sessionId) {
        InterviewSession session = getEntity(sessionId);
        if (session.getEndedAt() == null) {
            session.setEndedAt(Instant.now());
        }
        return InterviewSessionResponse.from(session);
    }

    public InterviewSessionResponse getById(UUID sessionId) {
        return InterviewSessionResponse.from(getEntity(sessionId));
    }

    public List<InterviewSessionResponse> getByUser(String userIdentifier) {
        return interviewSessionRepository.findByUserIdentifier(userIdentifier).stream()
                .map(InterviewSessionResponse::from)
                .toList();
    }

    /** 채점기와 결과 화면이 쓰는 뷰 — 세션 + 턴 + 답변을 한 번에. */
    public InterviewDetailResponse getDetail(UUID sessionId) {
        InterviewSession session = getEntity(sessionId);

        Map<UUID, InterviewAnswer> answersByTurnId = interviewAnswerRepository.findByTurnSessionId(sessionId).stream()
                .collect(Collectors.toMap(answer -> answer.getTurn().getId(), Function.identity()));

        List<InterviewDetailResponse.TurnWithAnswer> turns =
                interviewTurnRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).stream()
                        .map(turn -> {
                            InterviewAnswer answer = answersByTurnId.get(turn.getId());
                            return new InterviewDetailResponse.TurnWithAnswer(
                                    InterviewTurnResponse.from(turn),
                                    answer == null ? null : InterviewAnswerResponse.from(answer));
                        })
                        .toList();

        return new InterviewDetailResponse(InterviewSessionResponse.from(session), turns);
    }

    public InterviewSession getEntity(UUID sessionId) {
        return interviewSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "면접 세션을 찾을 수 없습니다: " + sessionId));
    }

    /** 끝난 세션에는 턴도 답변도 더 붙을 수 없다. */
    InterviewSession getOpenSession(UUID sessionId) {
        InterviewSession session = getEntity(sessionId);
        if (session.getEndedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 종료된 면접 세션입니다 (종료 시각 " + session.getEndedAt() + ")");
        }
        return session;
    }
}
